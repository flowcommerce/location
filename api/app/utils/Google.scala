package utils


import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.google.maps.{GeoApiContext, GeocodingApi, TimeZoneApi}
import com.google.maps.model.{AddressComponent, GeocodingResult, LatLng}
import io.flow.reference.{Countries, Timezones}
import io.flow.common.v0.models.Address
import io.flow.reference.v0.models.Timezone
import org.apache.commons.lang3.exception.ExceptionUtils
import play.api.Logger
import io.flow.google.places.v0.{models => Google}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Mostly copied from: https://github.com/flowcommerce/scrapbook/blob/master/lib/src/main/scala/geo/AddressExploration.scala
  */
@javax.inject.Singleton
class Google @javax.inject.Inject() (
  environmentVariables: EnvironmentVariables,
  system: ActorSystem
) {

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
          Logger.warn(s"Encountered the following error from the timezone API: ${ExceptionUtils.getStackTrace(e)}")
          None
        }
      }
    }
  }

  def getLocationsByAddress(address: String): Future[Seq[Address]] = {
    Future {
      Try {
        sortAddresses(
          parseResults(
            address = address,
            results = GeocodingApi.geocode(context, address).await().toList
          )
        )
      } match {
        case Success(result) => result
        case Failure(e) => {
          Logger.warn(s"Encountered the following error from the geocoding API: $e")
          Nil
        }
      }
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
    results.map { one =>
      val streetNumber = findAsString(one.addressComponents, Seq(Google.AddressComponentType.StreetNumber))
      val streetAddress = findAsString(one.addressComponents, Seq(Google.AddressComponentType.StreetAddress, Google.AddressComponentType.Route))
      val streets = Seq(streetNumber, streetAddress).filter(_.isDefined) match {
        case Nil => None
        case streets => Some(streets.flatten)
      }

      val postal = findAsString(one.addressComponents, Seq(Google.AddressComponentType.PostalCode))

      val country = findAsString(one.addressComponents, Seq(Google.AddressComponentType.Country)) match {
        case None => {
          Logger.warn(s"Could not determine country for address[$address]")
          None
        }

        case Some(q) => {
          Countries.find(q) match {
            case None => {
              Logger.warn(s"Country code[$q] was not valid in reference data. Skipping country")
              None
            }

            case Some(c) => {
              Some(c.iso31663)
            }
          }
        }
      }

      // best effort to find city/town name
      val city = Seq(
        Google.AddressComponentType.Locality,
        Google.AddressComponentType.Sublocality,
        Google.AddressComponentType.Neighborhood
      ).flatMap(typ => findAsString(one.addressComponents.toSeq, Seq(typ))).headOption

      val adminAreas = Seq(
        Google.AddressComponentType.AdministrativeAreaLevel1,
        Google.AddressComponentType.AdministrativeAreaLevel2,
        Google.AddressComponentType.AdministrativeAreaLevel3,
        Google.AddressComponentType.AdministrativeAreaLevel4,
        Google.AddressComponentType.AdministrativeAreaLevel5
      ).flatMap(typ => findAsString(one.addressComponents.toSeq, Seq(typ)))

      val (province, county) = adminAreas match {
        case Nil => (None, None)
        case a :: Nil => (Some(a), None)
        case a :: b :: Nil => (Some(a), Some(b))
        case a :: b :: more => {
          // TODO: Investigate what the other pieces are
          (Some(a), Some(b))
        }
      }

      Address(
        streets = streets,
        province = province,
        city = city,
        postal = postal,
        country = country,
        latitude = Some(one.geometry.location.lat.toString),
        longitude = Some(one.geometry.location.lng.toString)
      )
    }
  }

  private def findAsString(components: Seq[AddressComponent], types: Seq[Google.AddressComponentType]): Option[String] = {
    find(components, types) match {
      case Nil => None
      case one :: Nil => Some(one.longName)
      case multiple => Some(multiple.map(_.longName).mkString(" "))
    }
  }

  private def findAsShortString(components: Seq[AddressComponent], types: Seq[Google.AddressComponentType]): Option[String] = {
    find(components, types) match {
      case Nil => None
      case one :: Nil => Some(one.shortName)
      case multiple => Some(multiple.map(_.shortName).mkString(" "))
    }
  }

  private def find(components: Seq[AddressComponent], types: Seq[Google.AddressComponentType]): Seq[AddressComponent] = {
    components.filter { c =>
      c.types.map(t => Google.AddressComponentType(t.toString)).exists(types.contains)
    }
  }

}
