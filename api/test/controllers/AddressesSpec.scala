package controllers

import io.flow.common.v0.models.Address
import org.scalatestplus.play._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.test.Helpers._
import play.api.test._
import io.flow.location.v0.Client
import io.flow.common.v0.models.json._

import scala.concurrent.Await

class AddressesSpec extends PlaySpec with OneServerPerSuite with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  lazy val client = new Client(s"http://localhost:$port")

  "GET /addresses" in new WithServer {
    expectStatus(422) {
      client.addresses.get()
    }
  }

  "GET /addresses?address=sample" in new WithServer {
    expectStatus(422) {
      client.addresses.get(address = Some("sample"))
    }
  }

  "GET /addresses?ip=23.16.0.0" in new WithServer {
    val locations = await(
      client.addresses.get(ip = Some("23.16.0.0"))
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(locations).validate[Seq[Address]] match {
      case JsSuccess(c,_) => assert(true)
      case JsError(_) => assert(false)
    }
  }

  "POST /addresses/verifications" in new WithServer {
    expectErrors(
      client.addresses.postVerifications(address = Address())
    ).errors.map(_.message) must be(
      Seq("Address to verify cannot be empty")
    )
  }

  "POST /addresses/verifications with UK address" in new WithServer {
    val result = await(
      client.addresses.postVerifications(address = Address(text = Some("76 Belsize Park NW3-4NG")))
    )
    result.valid must be(true)
  }

}
