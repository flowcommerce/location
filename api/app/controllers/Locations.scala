package controllers

import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.location.v0.models.json._
import io.flow.common.v0.models.json._

class Locations extends Controller {
  def get(
    address: Option[String],
    ip: Option[String],
    latitude: Option[String],
    longitude: Option[String]
  ) = Action { request =>
    Helpers.getLocations(address, latitude, longitude, ip) match {
      case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      case Right(locations) => Ok(Json.toJson(locations))
    }
  }
}
