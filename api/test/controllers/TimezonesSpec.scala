package controllers

import io.flow.location.v0.Client
import io.flow.test.utils.FlowPlaySpec
import utils.DigitalElementSampleData

class TimezonesSpec extends FlowPlaySpec with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /addresses" in {
    expectStatus(422) {
      client.timezones.get()
    }
  }

  "GET /addresses?ip=23.16.0.0" in {
    val invalid = DigitalElementSampleData.IpTimezones.keys.toSeq.flatMap { ip =>
      val timezones = await(
        client.timezones.get(ip = Some("23.16.0.0"))
      )
      println(s"Timezones: $timezones")
      Some(s"Expected ip address '$ip' to resolve to timezone '${DigitalElementSampleData.IpTimezones(ip)}' but found '$timezones'")
    }

    invalid must equal(Nil)
  }

}
