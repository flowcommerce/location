package controllers

import io.flow.common.v0.models.Address
import io.flow.location.v0.models
import org.scalatestplus.play._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.test.Helpers._
import play.api.test._
import io.flow.location.v0.Client
import io.flow.common.v0.models.json._

import scala.concurrent.Await

class CountryDefaultsSpec extends PlaySpec with OneServerPerSuite with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  lazy val client = new Client(s"http://localhost:$port")

  val can = models.CountryDefaults("CAN", "CAD", "en")
  val fra = models.CountryDefaults("FRA", "EUR", "fr")
  val usa = models.CountryDefaults("USA", "USD", "en")

  "GET /geolocation/defaults/:country w/ invalid country" in new WithServer {
    expectStatus(404) {
      client.countryDefaults.getByCountry("other")
    }
  }

  "GET /geolocation/defaults/:country" in new WithServer {
    await(client.countryDefaults.getByCountry("usa")) must equal(usa)
    await(client.countryDefaults.getByCountry("can")) must equal(can)
    await(client.countryDefaults.getByCountry("fr")) must equal(fra)
  }

  "GET /geolocation/defaults" in new WithServer {
    val defaults = await(client.countryDefaults.get())
    defaults.find(_.country == "USA").get must equal(usa)
    defaults.find(_.country == "CAN").get must equal(can)
    defaults.find(_.country == "FRA").get must equal(fra)
  }
  
  "GET /geolocation/defaults?country=USA" in new WithServer {
    await(client.countryDefaults.get(country = Some("USA"))) must equal(Seq(usa))
  }
  
  "GET /geolocation/defaults?address=USA" in new WithServer {
    await(client.countryDefaults.get(address = Some("USA"))) must equal(Seq(usa))
  }
  
  "GET /geolocation/defaults?ip=23.16.0.0" in new WithServer {
    await(client.countryDefaults.get(ip = Some("23.16.0.0"))) must equal(Seq(can))
  }
  
}
