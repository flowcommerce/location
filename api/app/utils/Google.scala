package utils

import io.flow.common.v0.models.Address
import io.flow.reference.Countries
import io.flow.location.v0.models.Location
import com.google.maps.{GeoApiContext, GeocodingApi}
import com.google.maps.model.{AddressComponent, GeocodingResult}
import io.flow.play.util.Config

/**
  * Mostly copied from: https://github.com/flowcommerce/scrapbook/blob/master/lib/src/main/scala/geo/AddressExploration.scala
  */
object Google {
  val config = play.api.Play.current.injector.instanceOf[Config]
  val context = new GeoApiContext().setApiKey(config.requiredString("google.api.key"))

  def getLocationsByAddress(address: String): Either[Seq[String], Seq[Location]] = {
    GeocodingApi.geocode(context, address).await().toList match {
      case Nil => {
        Left(Seq(s"No results found in Google for address: [$address]"))
      }
      case results => {
        Right(parseResults(address, results))
      }
    }
  }

  private def parseResults(address: String, results: Seq[GeocodingResult]): Seq[Location] = {
    results.map { one =>
      val streetNumber = findAsString(one.addressComponents, Seq(Google.AddressComponentType.StreetNumber))
      val streetAddress = findAsString(one.addressComponents, Seq(Google.AddressComponentType.StreetAddress, Google.AddressComponentType.Route))
      val street = Seq(streetNumber, streetAddress).flatten.mkString(" ")
      val postal = findAsString(one.addressComponents, Seq(Google.AddressComponentType.PostalCode))

      val country = findAsString(one.addressComponents, Seq(Google.AddressComponentType.Country)) match {
        case None => sys.error("Google could not find country from address [$address]")
        case Some(q) => Countries.find(q) match {
          case None => sys.error(s"Reference lookup could not find country $q")
          case Some(c) => Some(c.iso31663)
        }
      }

      val city = findAsString(one.addressComponents, Seq(Google.AddressComponentType.Locality)) match {
        case Some(city) => Some(city)
        case None => findAsString(one.addressComponents, Seq(Google.AddressComponentType.Sublocality)) match {
          case Some(sublocality) => Some(sublocality)
          case None => findAsString(one.addressComponents, Seq(Google.AddressComponentType.Neighborhood)) match {
            case Some(neighborhood) => Some(neighborhood)
            case None => sys.error("Could not find city based on Google maps Locality, Sublocality, or Neighborhood")
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

      Location(
        address = Address(
          text = Some(address),
          streets = Some(Seq(street)),
          province = province,
          city = city,
          postal = postal,
          country = country
        ),
        latitude = one.geometry.location.lat.toString,
        longitude = one.geometry.location.lng.toString
      )
    }
  }

  private def findAsString(components: Seq[AddressComponent], types: Seq[AddressComponentType]): Option[String] = {
    find(components, types) match {
      case Nil => None
      case one :: Nil => Some(one.longName)
      case multiple => Some(multiple.map(_.longName).mkString(" "))
    }
  }

  private def findAsShortString(components: Seq[AddressComponent], types: Seq[AddressComponentType]): Option[String] = {
    find(components, types) match {
      case Nil => None
      case one :: Nil => Some(one.shortName)
      case multiple => Some(multiple.map(_.shortName).mkString(" "))
    }
  }

  private def find(components: Seq[AddressComponent], types: Seq[AddressComponentType]): Seq[AddressComponent] = {
    components.filter { c =>
      c.types.map( t => Google.AddressComponentType(t.toString) ).find(types.contains(_)) match {
        case None => false
        case Some(_) => true
      }
    }
  }

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
