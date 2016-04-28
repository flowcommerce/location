package controllers

import com.sanoma.cda.geoip.{IpLocation, MaxMindIpGeo}
import io.flow.common.v0.models.Address
import io.flow.location.v0.models.Location
import play.Play

object Helpers {
  val dbFilePath = Play.application().getFile("conf/GeoLite2-City.mmdb").getAbsolutePath
  val geoIp = MaxMindIpGeo(dbFilePath, 1000)

  def getByIp(ip: String) = {
    geoIp.getLocation(ip)
  }

  def validateRequestParameters(
    address: Option[String],
    latitude: Option[String],
    longitude: Option[String]): Option[Seq[String]] = {

    val addressError =
      address match {
        case Some (a) => Seq("[address] is not yet supported.")
        case None => Nil
      }

    val latitudeError =
      latitude match {
        case Some (a) => Seq("[latitude] is not yet supported.")
        case None => Nil
      }

    val longitudeError =
      longitude match {
        case Some (a) => Seq("[longitude] is not yet supported.")
        case None => Nil
      }

    (addressError ++ latitudeError ++ longitudeError).toList match {
      case Nil => None
      case errors => Some(errors)
    }
  }

  def getLocation(ipl: IpLocation): Either[Seq[String], Location] = {
    getLatLon(ipl) match {
      case None => Left(Seq(s"Could not geolocate IP"))
      case Some(geo) => {
        Right(
          Location(
            Address(
              city = ipl.city,
              province = ipl.region,
              postal = ipl.postalCode,
              country = getCountryCode(ipl)
            ),
            latitude = geo.latitude,
            longitude = geo.longitude
          )
        )
      }
    }
  }

  def getCountryCode(ipl: IpLocation): Option[String]= {
    ipl.countryName match {
      case Some(country) =>
        io.flow.reference.Countries.find(country) match {
          case Some(c) => Some(c.iso31663)
          case None => None
        }
      case None => None
    }
  }

  case class LatLong(latitude: String, longitude: String)

  def getLatLon(ipl: IpLocation): Option[LatLong] = {
    ipl.geoPoint.map { p =>
      LatLong(
        latitude = p.latitude.toString,
        longitude = p.longitude.toString
      )
    }
  }
}
