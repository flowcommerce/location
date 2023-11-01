package controllers

import io.flow.location.v0.models
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import io.flow.location.v0.Client
import io.flow.test.utils.FlowPlaySpec

class CountryDefaultsSpec extends FlowPlaySpec with GuiceOneServerPerSuite with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val client = new Client(wsClient, s"http://localhost:$port")

  private[this] val can = models.CountryDefaults("CAN", "CAD", "en")
  private[this] val fra = models.CountryDefaults("FRA", "EUR", "fr")
  private[this] val usa = models.CountryDefaults("USA", "USD", "en")

  "GET /geolocation/defaults/:country" in {
    await(client.countryDefaults.getByCountry("usa")) must equal(usa)
    await(client.countryDefaults.getByCountry("can")) must equal(can)
    await(client.countryDefaults.getByCountry("fr")) must equal(fra)
  }

  "GET /geolocation/defaults" in {
    val defaults = await(client.countryDefaults.get())
    defaults.find(_.country == "USA").get must equal(usa)
    defaults.find(_.country == "CAN").get must equal(can)
    defaults.find(_.country == "FRA").get must equal(fra)
  }

  "GET /geolocation/defaults?country=USA" in {
    await(client.countryDefaults.get(country = Some("USA"))) must equal(Seq(usa))
  }

  "GET /geolocation/defaults?ip=23.16.0.0" in {
    await(client.countryDefaults.get(ip = Some("23.16.0.0"))) must equal(Seq(can))
  }

}
