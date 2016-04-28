package controllers

import lib.Data
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
    Data.validateRequestParameters(address, latitude, longitude) match {
      case Some(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      case None =>
        ip match {
          case Some(ip) => Data.getByIp(ip) match {
            case Some(ipl) =>
              Ok(Json.toJson(Data.getLocation(ipl)))
            case None => UnprocessableEntity(Json.toJson(Validation.error(s"No location found for ip [$ip]")))
          }
          case None => UnprocessableEntity(Json.toJson(Validation.error("No valid query string parameters given.  Please provide an [ip]")))
        }
    }
  }
}
