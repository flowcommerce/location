package utils


import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.google.maps.model.{AddressComponent, ComponentFilter, GeocodingResult, LatLng}
import com.google.maps.{GeoApiContext, GeocodingApi, GeocodingApiRequest, TimeZoneApi}
import io.flow.common.v0.models.Address
import io.flow.google.places.v0.models.AddressComponentType
import io.flow.google.places.v0.{models => Google}
import io.flow.log.RollbarLogger
import io.flow.reference.v0.models.Timezone
import io.flow.reference.{Countries, Timezones}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Implicits {
  // Provides an easy way to extract address components from a geocoding result
  implicit class RichGeocodingResult(result: GeocodingResult) {
    // Extracts "Ontario" instead of "ON", etc.
    def extractLongName(types: AddressComponentType*): Option[String] = {
      findAsString(
        components = result.addressComponents.toIndexedSeq,
        types = types,
        extractor = _.longName
      )
    }

    // Extracts "ON" instead of "Ontario", etc.
    def extractShortName(types: AddressComponentType*): Option[String] = {
      findAsString(
        components = result.addressComponents.toIndexedSeq,
        types = types,
        extractor = _.shortName
      )
    }
  }

  private def findAsString(
    components: Seq[AddressComponent],
    types: Seq[Google.AddressComponentType],
    extractor: AddressComponent => String
  ): Option[String] = {
    find(components, types) match {
      case Nil => None
      case one :: Nil => Some(extractor(one))
      case multiple => Some(multiple.map(extractor).mkString(" "))
    }
  }

  private def find(components: Seq[AddressComponent], types: Seq[Google.AddressComponentType]): Seq[AddressComponent] = {
    components.filter { c =>
      c.types.map(t => Google.AddressComponentType(t.toString)).exists(types.contains)
    }
  }
}

/**
  * Mostly copied from: https://github.com/flowcommerce/scrapbook/blob/master/lib/src/main/scala/geo/AddressExploration.scala
  */
@javax.inject.Singleton
class Google @javax.inject.Inject() (
  environmentVariables: EnvironmentVariables,
  system: ActorSystem,
  logger: RollbarLogger
) {
  import Implicits._

  private[this] val context = new GeoApiContext.Builder()
    .connectTimeout(1000, TimeUnit.MILLISECONDS)
    .readTimeout(1000, TimeUnit.MILLISECONDS)
    .apiKey(environmentVariables.googleApiKey)
    .build()

  private[this] implicit val ec = system.dispatchers.lookup("google-api-context")

  def getTimezone(lat: Double, lng: Double): Future[Option[Timezone]] = {
    Future {
      // returns java.util.TimeZone, which has getID()
      Try {
        val tz = TimeZoneApi.getTimeZone(context, new LatLng(lat, lng)).await()
        Timezones.find(tz.getID)
      } match {
        case Success(result) => result
        case Failure(e) => {
          logger.warn(s"Encountered the following error from the timezone API", e)
          None
        }
      }
    }
  }

  def getLocationsByAddress(address: String, components: Option[String]): Future[Seq[Address]] = {
    val baseRequest = GeocodingApi.geocode(context, address)
    val componentFilters: Seq[ComponentFilter] = getComponentFilters(components)
    val geocodingApiRequest: GeocodingApiRequest = componentFilters.toList match {
      case Nil => baseRequest
      case filters => baseRequest.components(filters:_*)
    }

    Future {
      Try {
        sortAddresses(
          parseResults(
            address = address,
            results = geocodingApiRequest.await().toList
          )
        )
      } match {
        case Success(result) => result
        case Failure(e) => {
          logger.warn(s"Encountered the following error from the geocoding API", e)
          Nil
        }
      }
    }
  }

  /**
   * Google Geocoding Component Filtering
   * https://developers.google.com/maps/documentation/javascript/geocoding#ComponentFiltering
   */
  private[utils] def getComponentFilters(components: Option[String]): Seq[ComponentFilter] = {
    components match {
      case None => Nil
      case Some(comps) => comps.split("\\|").toList.flatMap(_.split(":") match {
        case Array(key, value) =>
          key match {
            case "country" => Some(ComponentFilter.country(value))
            case "postal_code" => Some(ComponentFilter.postalCode(value))
            case "postal_code_prefix" => Some(new ComponentFilter("postal_code_prefix", value))
            case "route" => Some(ComponentFilter.route(value))
            case "locality" => Some(ComponentFilter.locality(value))
            case "administrative_area" => Some(ComponentFilter.administrativeArea(value))
            case _ =>
              logger
                .fingerprint(this.getClass.getName)
                .withKeyValue("component_filter_key", key)
                .withKeyValue("component_filter_value", value)
                .info("Unsupported component filter key")
              None
          }
      })
    }
  }

  /**
    * Ensures addresses w/ countries are defined earlier in list
    */
  private[this] def sortAddresses(addresses: Seq[Address]): Seq[Address] = {
    sortByPostalCode(addresses.filter(_.country.isDefined)) ++ sortByPostalCode(addresses.filter(_.country.isEmpty))
  }

  /**
    * Prefer longer postal code as that indicates more precision
    */
  private[this] def sortByPostalCode(addresses: Seq[Address]): Seq[Address] = {
    addresses.sortBy { a => a.postal.map(_.length).getOrElse(0) }.reverse
  }

  private[this] def parseResults(address: String, results: Seq[GeocodingResult]): Seq[Address] = {
    results.map { geocodingResult =>
      val streetNumber = geocodingResult.extractLongName(Google.AddressComponentType.StreetNumber)
      val streetAddress = geocodingResult.extractLongName(
        Seq(
          Google.AddressComponentType.StreetAddress,
          Google.AddressComponentType.Route
        ):_*
      )

      val streets = Some(Seq(streetNumber, streetAddress).flatten).filter(_.nonEmpty)
      val postal = geocodingResult.extractLongName(Google.AddressComponentType.PostalCode)
      val country = geocodingResult
        .extractLongName(Google.AddressComponentType.Country)
        .flatMap(Countries.find)
        .map(_.iso31663) orElse {
          logger.withKeyValue("address",address).info(s"Could not determine country for address or the country code was not valid")
          None
        }

      // best effort to find city/town name
      val city = Seq(
        Google.AddressComponentType.Locality,
        Google.AddressComponentType.Sublocality,
        Google.AddressComponentType.Neighborhood
      ).flatMap(geocodingResult.extractLongName(_)).headOption

      val adminAreas = Seq(
        Google.AddressComponentType.AdministrativeAreaLevel1,
        Google.AddressComponentType.AdministrativeAreaLevel2,
        Google.AddressComponentType.AdministrativeAreaLevel3,
        Google.AddressComponentType.AdministrativeAreaLevel4,
        Google.AddressComponentType.AdministrativeAreaLevel5
      ).flatMap(geocodingResult.extractShortName(_))

      // `adminAreas` will contain province or state, then county, others - we just need province here
      val provinceOrState = adminAreas.headOption

      Address(
        streets = streets,
        streetNumber = streetNumber,
        province = provinceOrState,
        city = city,
        postal = postal,
        country = country,
        latitude = Some(geocodingResult.geometry.location.lat.toString),
        longitude = Some(geocodingResult.geometry.location.lng.toString)
      )
    }
  }
}
