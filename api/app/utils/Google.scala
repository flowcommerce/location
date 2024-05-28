package utils

import java.util.concurrent.TimeUnit

import com.google.maps.PendingResult.Callback
import com.google.maps._
import com.google.maps.model.{AddressComponent, ComponentFilter, GeocodingResult, LatLng}
import io.flow.common.v0.models.Address
import io.flow.google.places.v0.models.AddressComponentType
import io.flow.google.places.v0.{models => Google}
import io.flow.log.RollbarLogger
import io.flow.reference.v0.models.Timezone
import io.flow.reference.{Countries, Timezones}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

object Implicits {
  // Provides an easy way to extract address components from a geocoding result
  implicit class RichGeocodingResult(val result: GeocodingResult) extends AnyVal {
    // Extracts "Ontario" instead of "ON", etc.
    def extractLongName(types: AddressComponentType*): Option[String] = {
      findAsString(
        components = result.addressComponents.toIndexedSeq,
        types = types,
        extractor = _.longName,
      )
    }

    // Extracts "ON" instead of "Ontario", etc.
    def extractShortName(types: AddressComponentType*): Option[String] = {
      findAsString(
        components = result.addressComponents.toIndexedSeq,
        types = types,
        extractor = _.shortName,
      )
    }
  }

  private def findAsString(
    components: Seq[AddressComponent],
    types: Seq[Google.AddressComponentType],
    extractor: AddressComponent => String,
  ): Option[String] = {
    find(components, types) match {
      case Nil => None
      case one :: Nil => Some(extractor(one))
      case multiple => Some(multiple.map(extractor).mkString(" "))
    }
  }

  private def find(
    components: Seq[AddressComponent],
    types: Seq[Google.AddressComponentType],
  ): Seq[AddressComponent] = {
    components.filter { c =>
      c.types.map(t => Google.AddressComponentType(t.toString)).exists(types.contains)
    }
  }

  implicit class PendingResultScala[T](val pendingRes: PendingResult[T]) extends AnyVal {

    def runAsync(): Future[T] = {
      val p = Promise[T]()
      pendingRes.setCallback(new Callback[T] {
        override def onResult(result: T): Unit = p.success(result)
        override def onFailure(e: Throwable): Unit = p.failure(e)
      })
      p.future
    }

  }

}

/** Mostly copied from:
  * https://github.com/flowcommerce/scrapbook/blob/main/lib/src/main/scala/geo/AddressExploration.scala
  */
@javax.inject.Singleton
class Google @javax.inject.Inject() (
  environmentVariables: EnvironmentVariables,
  rollbar: RollbarLogger,
)(implicit ec: ExecutionContext) {
  import Implicits._

  private[this] val logger = rollbar.fingerprint(getClass.getName)

  private[this] val context = new GeoApiContext.Builder()
    .connectTimeout(1000, TimeUnit.MILLISECONDS)
    .readTimeout(1000, TimeUnit.MILLISECONDS)
    .apiKey(environmentVariables.googleApiKey.getOrElse("asdfsdfsdfads"))
    .build()

  def getTimezone(lat: Double, lng: Double): Future[Option[Timezone]] =
    TimeZoneApi
      .getTimeZone(context, new LatLng(lat, lng))
      .runAsync()
      .map(tz => Timezones.find(tz.getID))
      .recover { case NonFatal(e) =>
        logger.warn("Encountered the following error from the timezone API", e)
        None
      }

  def getLocationsByAddress(
    address: String,
    country: Option[String],
    postalPrefix: Option[String],
  ): Future[Seq[Address]] = {
    val baseRequest = GeocodingApi.geocode(context, address)
    val componentFilters: Seq[ComponentFilter] = getComponentFilters(country, postalPrefix)
    val geocodingApiRequest: GeocodingApiRequest =
      if (componentFilters.isEmpty) baseRequest else baseRequest.components(componentFilters: _*)

    geocodingApiRequest
      .runAsync()
      .map { addresses =>
        val parsed = parseResults(address = address, results = addresses.toSeq)
        sortAddresses(parsed)
      }
      .recover { case NonFatal(e) =>
        logger.warn("Encountered the following error from the geocoding API", e)
        Nil
      }
  }

  /** Google Geocoding Component Filtering
    * https://developers.google.com/maps/documentation/javascript/geocoding#ComponentFiltering
    */
  private[utils] def getComponentFilters(
    country: Option[String],
    postalPrefix: Option[String],
  ): Seq[ComponentFilter] = {
    val countryCodeFilter: Option[ComponentFilter] = country.flatMap { c =>
      Countries.find(c).map(_.iso31662).map(ComponentFilter.country)
    }
    val postalCodePrefixFilter: Option[ComponentFilter] = postalPrefix.map(new ComponentFilter("postal_code_prefix", _))

    Seq(countryCodeFilter, postalCodePrefixFilter).flatten
  }

  /** Ensures addresses w/ countries are defined earlier in list
    */
  private[this] def sortAddresses(addresses: Seq[Address]): Seq[Address] = {
    sortByPostalCode(addresses.filter(_.country.isDefined)) ++ sortByPostalCode(addresses.filter(_.country.isEmpty))
  }

  /** Prefer longer postal code as that indicates more precision
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
          Google.AddressComponentType.Route,
        ): _*,
      )

      val streets = Some(Seq(streetNumber, streetAddress).flatten).filter(_.nonEmpty)
      val postal = geocodingResult.extractLongName(Google.AddressComponentType.PostalCode)
      val country = geocodingResult
        .extractLongName(Google.AddressComponentType.Country)
        .flatMap(Countries.find)
        .map(_.iso31663) orElse {
        logger
          .withKeyValue("address", address)
          .info("Could not determine country for address or the country code was not valid")
        None
      }

      // best effort to find city/town name
      val city = Seq(
        Google.AddressComponentType.Locality,
        Google.AddressComponentType.Sublocality,
        Google.AddressComponentType.Neighborhood,
      ).flatMap(geocodingResult.extractLongName(_)).headOption

      val adminAreas = Seq(
        Google.AddressComponentType.AdministrativeAreaLevel1,
        Google.AddressComponentType.AdministrativeAreaLevel2,
        Google.AddressComponentType.AdministrativeAreaLevel3,
        Google.AddressComponentType.AdministrativeAreaLevel4,
        Google.AddressComponentType.AdministrativeAreaLevel5,
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
        longitude = Some(geocodingResult.geometry.location.lng.toString),
      )
    }
  }
}
