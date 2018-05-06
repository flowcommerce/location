package controllers

import io.flow.common.v0.models.Address
import play.api.libs.json.{JsError, JsSuccess, Json}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import io.flow.location.v0.Client
import io.flow.common.v0.models.json._
import io.flow.test.utils.FlowPlaySpec

class AddressesSpec extends FlowPlaySpec with GuiceOneServerPerSuite with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /addresses without an IP returns a proper message" in {
    expectErrors(
      client.addresses.get(ip = None)
    ).genericError.messages must be(
      Seq("Must specify 'ip' parameter")
    )
  }

  "GET /addresses?ip=23.16.0.0" in {
    val locations = await(
      client.addresses.get(ip = Some("23.16.0.0"))
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(locations).validate[Seq[Address]] match {
      case JsSuccess(c,_) => assert(true)
      case JsError(_) => assert(false)
    }
  }

  "POST /addresses/verifications" in {
    expectErrors(
      client.addresses.postVerifications(address = Address())
    ).genericError.messages must be(
      Seq("Address to verify cannot be empty")
    )
  }

  "POST /addresses/verifications with UK address" in {
    val result = await(
      client.addresses.postVerifications(address = Address(text = Some("76 Belsize Park NW3-4NG")))
    )
    result.valid must be(true)
  }

}
