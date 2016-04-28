package lib

import com.sanoma.cda.geo.Point
import com.sanoma.cda.geoip.IpLocation
import controllers.Helpers
import io.flow.common.v0.models.Address
import io.flow.location.v0.models.Location
import org.scalatestplus.play._

class DataSpec extends PlaySpec with OneAppPerSuite {

  "Data" should {
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
      val (lat, lon) = Helpers.getLatLon(validIpLocation)

      lat must equal("49.7333")
      lon must equal("-114.8853")
    }

    "return empty latitude/longitude when ipLocation is invalid" in {
      val (lat, lon) = Helpers.getLatLon(invalidIpLocation)

      lat must equal("")
      lon must equal("")
    }

    "return valid country 3 character iso code when ipLocation is valid" in {
      Helpers.getCountryCode(validIpLocation) must equal(Some("CAN"))
    }

    "return no country code when ipLocation is invalid" in {
      Helpers.getCountryCode(invalidIpLocation) must equal(None)
    }

    "return valid location when ipLocation is valid" in {
      Helpers.getLocation(validIpLocation) must equal(Location(Address(None,None,Some("Sparwood"),None,None,Some("CAN")),"49.7333","-114.8853"))
    }
  }
}
