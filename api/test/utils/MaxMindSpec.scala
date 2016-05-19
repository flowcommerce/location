package utils

import com.sanoma.cda.geo.Point
import com.sanoma.cda.geoip.IpLocation
import io.flow.common.v0.models.Location
import org.scalatestplus.play._

class MaxMindSpec extends PlaySpec with OneAppPerSuite {

  "MaxMind" should {
    val validIpLocation = IpLocation(
      countryCode = Some("CA"),
      countryName = Some("Canada"),
      region = None,
      city = Some("Sparwood"),
      geoPoint = Some(Point(49.7333, -114.8853)),
      postalCode = None,
      continent = None)

    val invalidIpLocation = IpLocation(
      countryCode = None,
      countryName = None,
      region = None,
      city = None,
      geoPoint = None,
      postalCode = None,
      continent = None)

    "return valid latitude/longitude when ipLocation is valid" in {
      val geo = MaxMind.getLatLong(validIpLocation).getOrElse {
        sys.error("Failed to resolve known IP")
      }

      geo.latitude must equal("49.7333")
      geo.longitude must equal("-114.8853")
    }

    "return empty latitude/longitude when ipLocation is invalid" in {
      MaxMind.getLatLong(invalidIpLocation) must be(None)
    }

    "return valid country 3 character iso code when ipLocation is valid" in {
      MaxMind.getCountryCode(validIpLocation) must equal(Some("CAN"))
    }

    "return no country code when ipLocation is invalid" in {
      MaxMind.getCountryCode(invalidIpLocation) must equal(None)
    }

    "return valid location when ipLocation is valid" in {
      MaxMind.getLocation(validIpLocation) must equal(
        Right(Location(None,None,Some("Sparwood"),None,None,Some("CAN"),Some("49.7333"),Some("-114.8853")))
      )
    }
  }
}
