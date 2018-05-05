package controllers

import io.flow.common.v0.models.Address
import io.flow.common.v0.models.json._
import io.flow.location.v0.Client
import io.flow.reference.data.Timezones
import io.flow.test.utils.FlowPlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import utils.DigitalElement

class TimezonesSpec extends FlowPlaySpec with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /addresses" in {
    expectStatus(422) {
      client.timezones.get()
    }
  }

  "GET /addresses?ip=23.16.0.0" in {
    val data = Map(
      "37.187.99.146" -> Timezones.EuropeParis,
      "23.16.0.0" -> Timezones.AmericaEdmonton,
      "185.236.200.99" -> Timezones.AmericaLosAngeles,
      "173.63.71.187" -> Timezones.AmericaNewYork
    )

    val invalid = data.keys.toSeq.flatMap { ip =>
      val timezones = await(
        client.timezones.get(ip = Some("23.16.0.0"))
      )
      println(s"Timezones: $timezones")
      Some(s"Expected ip address '$ip' to resolve to timezone '${data(ip)}' but found '$timezones'")
    }

    invalid must equal(Nil)
  }

}
