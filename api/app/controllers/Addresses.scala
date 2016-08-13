package controllers

import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.common.v0.models.json._
import scala.concurrent.Future

@javax.inject.Singleton
class Addresses @javax.inject.Inject() (
  helpers: Helpers
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    address: Option[String],
    ip: Option[String]
  ) = Action.async { request =>
    Future {
      helpers.getLocations(address = address, ip = ip) match {
        case Left(_) => UnprocessableEntity(Json.toJson(Validation.error("Must specify either 'address' or 'ip'")))
        case Right(locations) => Ok(Json.toJson(locations))
      }
    }
  }

}
