package utils


import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import com.google.maps.{GeoApiContext, GeocodingApi, TimeZoneApi}
import com.google.maps.model.{AddressComponent, GeocodingResult, LatLng}
import io.flow.reference.{Countries, Timezones}
import io.flow.common.v0.models.Address
import io.flow.reference.v0.models.Timezone
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Google {

  sealed trait AddressComponentType
  object AddressComponentType {
    case object AdministrativeAreaLevel1 extends AddressComponentType { override def toString() = "administrative_area_level_1" }
    case object AdministrativeAreaLevel2 extends AddressComponentType { override def toString() = "administrative_area_level_2" }
    case object AdministrativeAreaLevel3 extends AddressComponentType { override def toString() = "administrative_area_level_3" }
    case object AdministrativeAreaLevel4 extends AddressComponentType { override def toString() = "administrative_area_level_4" }
    case object AdministrativeAreaLevel5 extends AddressComponentType { override def toString() = "administrative_area_level_5" }
    case object AIRPORT extends AddressComponentType { override def toString() = "airport" }
    case object BUS_STATION extends AddressComponentType { override def toString() = "bus_station" }
    case object COLLOQUIAL_AREA extends AddressComponentType { override def toString() = "colloquial_area" }
    case object Country extends AddressComponentType { override def toString() = "country" }
    case object ESTABLISHMENT extends AddressComponentType { override def toString() = "establishment" }
    case object FLOOR extends AddressComponentType { override def toString() = "floor" }
    case object INTERSECTION extends AddressComponentType { override def toString() = "intersection" }
    case object Locality extends AddressComponentType { override def toString() = "locality" }
    case object NATURAL_FEATURE extends AddressComponentType { override def toString() = "natural_feature" }
    case object Neighborhood extends AddressComponentType { override def toString() = "neighborhood" }
    case object PARK extends AddressComponentType { override def toString() = "park" }
    case object PARKING extends AddressComponentType { override def toString() = "parking" }
    case object POINT_OF_INTEREST extends AddressComponentType { override def toString() = "point_of_interest" }
    case object POLITICAL extends AddressComponentType { override def toString() = "political" }
    case object PostBox extends AddressComponentType { override def toString() = "post_box" }
    case object PostalCode extends AddressComponentType { override def toString() = "postal_code" }
    case object PostalCodePrefix extends AddressComponentType { override def toString() = "postal_code_prefix" }
    case object PostalCodeSuffix extends AddressComponentType { override def toString() = "postal_code_suffix" }
    case object POSTAL_TOWN extends AddressComponentType { override def toString() = "postal_town" }
    case object PREMISE extends AddressComponentType { override def toString() = "premise" }
    case object ROOM extends AddressComponentType { override def toString() = "room" }
    case object Route extends AddressComponentType { override def toString() = "route" }
    case object StreetAddress extends AddressComponentType { override def toString() = "street_address" }
    case object StreetNumber extends AddressComponentType { override def toString() = "street_number" }
    case object Sublocality extends AddressComponentType { override def toString() = "sublocality" }
    case object SUBLOCALITY_LEVEL_1  extends AddressComponentType { override def toString() = "sublocality_level_1" }
    case object SUBLOCALITY_LEVEL_2  extends AddressComponentType { override def toString() = "sublocality_level_2" }
    case object SUBLOCALITY_LEVEL_3  extends AddressComponentType { override def toString() = "sublocality_level_3" }
    case object SUBLOCALITY_LEVEL_4  extends AddressComponentType { override def toString() = "sublocality_level_4" }
    case object SUBLOCALITY_LEVEL_5  extends AddressComponentType { override def toString() = "sublocality_level_5" }
    case object SUBPREMISE extends AddressComponentType { override def toString() = "subpremise" }
    case object TRAIN_STATION extends AddressComponentType { override def toString() = "train_station" }
    case object TRANSIT_STATION extends AddressComponentType { override def toString() = "transit_station" }
    case object UNKNOWN extends AddressComponentType { override def toString() = "unknown" }

    val all = Seq(AdministrativeAreaLevel1, AdministrativeAreaLevel2, AdministrativeAreaLevel3, AdministrativeAreaLevel4, AdministrativeAreaLevel5, AIRPORT, BUS_STATION, COLLOQUIAL_AREA, Country, ESTABLISHMENT, FLOOR, INTERSECTION, Locality, NATURAL_FEATURE, Neighborhood, PARK, PARKING, POINT_OF_INTEREST, POLITICAL, PostBox, PostalCode, PostalCodePrefix, PostalCodeSuffix, POSTAL_TOWN, PREMISE, ROOM, Route, StreetAddress, StreetNumber, Sublocality, SUBLOCALITY_LEVEL_1, SUBLOCALITY_LEVEL_2, SUBLOCALITY_LEVEL_3, SUBLOCALITY_LEVEL_4, SUBLOCALITY_LEVEL_5, SUBPREMISE, TRAIN_STATION, TRANSIT_STATION, UNKNOWN)
    private[this] val byName = all.map(x => x.toString.toLowerCase -> x).toMap
    def apply(value: String): AddressComponentType = fromString(value).getOrElse(UNKNOWN)
    def fromString(value: String): Option[AddressComponentType] = byName.get(value.toLowerCase)

  }

}

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
        Timezones.find(tz.getID())
      } match {
        case Success(result) => result
        case Failure(e) => {
          Logger.warn(s"Encountered the following error from the timezone API: $e")
          None
        }
      }
    }
  }

  def getLocationsByAddress(address: String): Future[Seq[Address]] = {
    Future {
      Try {
        GeocodingApi.geocode(context, address).await().toList match {
          case Nil => {
            Nil
          }
          case results => {
            sortAddresses(parseResults(address, results))
          }
        }
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
      val city = findAsString(one.addressComponents, Seq(Google.AddressComponentType.Locality)) match {
        case Some(city) => Some(city)
        case None => findAsString(one.addressComponents, Seq(Google.AddressComponentType.Sublocality)) match {
          case Some(sublocality) => Some(sublocality)
          case None => findAsString(one.addressComponents, Seq(Google.AddressComponentType.Neighborhood)) match {
            case Some(neighborhood) => Some(neighborhood)
            case None => None
          }
        }
      }

      val adminAreas = Seq(
        findAsString(one.addressComponents, Seq(Google.AddressComponentType.AdministrativeAreaLevel1)),
        findAsString(one.addressComponents, Seq(Google.AddressComponentType.AdministrativeAreaLevel2)),
        findAsString(one.addressComponents, Seq(Google.AddressComponentType.AdministrativeAreaLevel3)),
        findAsString(one.addressComponents, Seq(Google.AddressComponentType.AdministrativeAreaLevel4)),
        findAsString(one.addressComponents, Seq(Google.AddressComponentType.AdministrativeAreaLevel5))
      ).flatten

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
      c.types.map( t => Google.AddressComponentType(t.toString) ).find(types.contains(_)) match {
        case None => false
        case Some(_) => true
      }
    }
  }

}
