package controllers

import io.flow.location.v0.Client
import io.flow.test.utils.FlowPlaySpec
import utils.DigitalElementSampleData

class TimezonesSpec extends FlowPlaySpec with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /addresses" in {
    expectErrors {
      client.timezones.get(ip = None)
    }.genericError.messages must equal(
      Seq("Must specify 'ip' parameter")
    )
  }

  "GET /addresses for unknown IPs" in {
    expectErrors {
      client.timezones.get(ip = Some("1.2.3.4"))
    }.genericError.messages must equal(
      Seq("Timezone information not available for ip '1.2.3.4'")
    )
  }

  "GET /addresses for valid IPs" in {
    val invalid = DigitalElementSampleData.IpTimezones.keys.toSeq.flatMap { ip =>
      val timezone = await(
        client.timezones.get(ip = Some(ip))
      ).headOption.getOrElse {
        sys.error("Expected 1 timezone")
      }
      val expected = DigitalElementSampleData.IpTimezones(ip)
      if (expected == timezone) {
        None
      } else {
        Some(s"Expected ip address '$ip' to resolve to timezone '${expected.name}' but found '${timezone.name}'")
      }
    }

    invalid must equal(Nil)
  }

}