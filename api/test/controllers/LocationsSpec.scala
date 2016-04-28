package controllers

import io.flow.location.v0.models.Location
import org.scalatestplus.play._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.test.Helpers._
import play.api.test._
import io.flow.location.v0.Client
import io.flow.location.v0.models.json._

import scala.concurrent.Await

class LocationsSpec extends PlaySpec with OneServerPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  lazy val client = new Client(s"http://localhost:$port")

  "GET /locations" in new WithServer {
    expectStatus(422) {
      Await.result(client.locations.get(), DefaultDuration)
    }
  }

  "GET /locations?address=sample" in new WithServer {
    expectStatus(422) {
      Await.result(client.locations.get(address = Some("sample")), DefaultDuration)
    }
  }

  "GET /locations?latitude=sample" in new WithServer {
    expectStatus(422) {
      Await.result(client.locations.get(latitude = Some("sample")), DefaultDuration)
    }
  }

  "GET /locations?longitude=sample" in new WithServer {
    expectStatus(422) {
      Await.result(client.locations.get(longitude = Some("sample")), DefaultDuration)
    }
  }

  "GET /locations?ip=23.16.0.0" in new WithServer {
    val location = await(
      client.locations.get(ip = Some("23.16.0.0"))
    )

    // a bit redundant to serialize and deserialize, but makes the point of validating models as proper Json
    Json.toJson(location).validate[Location] match {
      case JsSuccess(c,_) => assert(true)
      case JsError(_) => assert(false)
    }
  }

  /*"GET /countries by query" in new WithServer {
    await(
      client.countries.get(q = Some("usa"))
    ).map(_.name) must be(Seq("United States"))

    await(
      client.countries.get(q = Some("us"))
    ).map(_.name) must be(Seq("United States"))

    await(
      client.countries.get(q = Some(" united states "))
    ).map(_.name) must be(Seq("United States"))
  }*/

}
