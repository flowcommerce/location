package controllers

import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.common.v0.models.json._
import scala.concurrent.Future

@javax.inject.Singleton
class Locations @javax.inject.Inject() (
  helpers: Helpers
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    address: Option[String],
    ip: Option[String],
    latitude: Option[String],
    longitude: Option[String]
  ) = Action.async { request =>
    Future {
      helpers.getLocations(address, latitude, longitude, ip) match {
        case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
        case Right(locations) => Ok(Json.toJson(locations))
      }
    }
  }

}
