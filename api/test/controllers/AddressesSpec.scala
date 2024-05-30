package controllers

import io.flow.common.v0.models.Address
import io.flow.common.v0.models.json._
import io.flow.location.v0.Client
import io.flow.location.v0.models.LocationErrorCode
import io.flow.reference.data.Countries
import io.flow.test.utils.FlowPlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsSuccess, Json}

class AddressesSpec extends FlowPlaySpec with GuiceOneServerPerSuite with TestHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /addresses without an IP returns a proper message" in {
    expectUnprocessableEntity(
      client.addresses.get(ip = None),
    ).status mustBe UnprocessableEntityStatusCode
  }

  "GET /addresses?ip=23.16.0.0" in {
    val locations = await(
      client.addresses.get(ip = Some("23.16.0.0")),
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(locations).validate[Seq[Address]] mustBe a[JsSuccess[_]]
  }

  "GET /addresses?ip=2001:470:1f0b:079c::" in {
    val locations = await(
      client.addresses.get(ip = Some("2001:470:1f0b:079c::")),
    )

    Json.toJson(locations).validate[Seq[Address]] mustBe a[JsSuccess[_]]
  }

  "GET /addresses?ip=::ffff:106:0" in {
    val locations = await(
      client.addresses.get(ip = Some("::ffff:106:0")),
    )

    Json.toJson(locations).validate[Seq[Address]] mustBe a[JsSuccess[_]]
    locations.map(_.country.get).contains("IND")
  }

  "GET /addresses?address=190 Japan&country_code=Japan&postal_code_prefix=190" in {
    val locations = await(
      client.addresses.get(
        address = Some(s"190 ${Countries.Jpn.name}"),
        country = Some(Countries.Jpn.name),
        postalPrefix = Some("190"),
      ),
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(locations).validate[Seq[Address]] mustBe a[JsSuccess[_]]
  }

  "GET /addresses?address=190 Japan" in {
    val locations = await(
      client.addresses.get(address = Some(s"190 ${Countries.Jpn.name}")),
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(locations).validate[Seq[Address]] mustBe a[JsSuccess[_]]
  }

  "POST /addresses/verifications" in {
    expectErrors(LocationErrorCode.AddressRequired)(
      client.addresses.postVerifications(address = Address()),
    ).messages mustBe Seq("Address to verify cannot be empty")
  }

  "POST /addresses/verifications with UK address" in {
    val result = await(
      client.addresses.postVerifications(address = Address(text = Some("76 Belsize Park NW3-4NG"))),
    )
    result.valid mustBe true
  }

}
