package utils

import com.sanoma.cda.geoip.{IpLocation, MaxMindIpGeo}
import io.flow.common.v0.models.Address
import io.flow.reference.v0.models.Country

private[utils] case class LatLong(latitude: String, longitude: String)

object MaxMind {
  import sys.process._
  import java.net.URL
  import java.io.File
  import scala.language.postfixOps

  new URL("https://s3.amazonaws.com/io.flow.aws-s3-public/location/GeoLite2-City.mmdb") #> new File("GeoLite2-City.mmdb") !!

  val geoIp = MaxMindIpGeo("GeoLite2-City.mmdb", 1000)

  def getByIp(ip: String): Option[Address] = {
    geoIp.getLocation(ip).map { ipl =>
      address(ipl)
    }
  }

  private[utils] def address(ipl: IpLocation): Address = {
    val latLong = MaxMind.latLong(ipl)
    Address(
      city = ipl.city,
      province = ipl.region,
      postal = ipl.postalCode,
      country = MaxMind.country(ipl).map(_.iso31663),
      latitude = latLong.map(_.latitude),
      longitude = latLong.map(_.longitude)
    )
  }
  
  private[utils] def country(ipl: IpLocation): Option[Country]= {
    ipl.countryCode.flatMap { c =>
      io.flow.reference.Countries.find(c)
    }
  }

  private[utils] def latLong(ipl: IpLocation): Option[LatLong] = {
    ipl.geoPoint.map { p =>
      LatLong(
        latitude = p.latitude.toString,
        longitude = p.longitude.toString
      )
    }
  }

}
