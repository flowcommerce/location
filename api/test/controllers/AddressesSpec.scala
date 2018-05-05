package controllers

import io.flow.common.v0.models.Address
import play.api.libs.json.{JsError, JsSuccess, Json}
import io.flow.location.v0.Client
import io.flow.common.v0.models.json._
import io.flow.test.utils.FlowPlaySpec

class AddressesSpec extends FlowPlaySpec with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /addresses" in {
    expectStatus(422) {
      client.addresses.get()
    }
  }

  "GET /addresses?ip=23.16.0.0" in {
    val locations = await(
      client.addresses.get(ip = Some("23.16.0.0"))
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(locations).validate[Seq[Address]] match {
      case JsSuccess(_,_) => // no-op
      case JsError(_) => sys.error("Failed to serialize and deserialize address")
    }
  }

}
