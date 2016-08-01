package utils

import com.sanoma.cda.geoip.{IpLocation, MaxMindIpGeo}
import io.flow.common.v0.models.Address

case class LatLong(latitude: String, longitude: String)

object MaxMind {
  import sys.process._
  import java.net.URL
  import java.io.File
  import scala.language.postfixOps
  new URL("https://s3.amazonaws.com/io.flow.aws-s3-public/location/GeoLite2-City.mmdb") #> new File("GeoLite2-City.mmdb") !!

  val geoIp = MaxMindIpGeo("GeoLite2-City.mmdb", 1000)

  def getByIp(ip: String): Option[IpLocation] = {
    geoIp.getLocation(ip)
  }

  def getCountryCode(ipl: IpLocation): Option[String]= {
    ipl.countryCode match {
      case Some(country) =>
        io.flow.reference.Countries.find(country) match {
          case Some(c) => Some(c.iso31663)
          case None => None
        }
      case None => None
    }
  }

  def getLatLong(ipl: IpLocation): Option[LatLong] = {
    ipl.geoPoint.map { p =>
      LatLong(
        latitude = p.latitude.toString,
        longitude = p.longitude.toString
      )
    }
  }

  def getLocation(ipl: IpLocation): Either[Seq[String], Address] = {
    MaxMind.getLatLong(ipl) match {
      case None => {
        Left(Seq(s"Could not geolocate IP"))
      }

      case Some(geo) => {
        Right(
          Address(
            city = ipl.city,
            province = ipl.region,
            postal = ipl.postalCode,
            country = MaxMind.getCountryCode(ipl),
            latitude = Some(geo.latitude),
            longitude = Some(geo.longitude)
          )
        )
      }
    }
  }
}