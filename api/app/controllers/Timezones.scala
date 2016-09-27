package controllers

import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.reference.v0.models.json._
import io.flow.error.v0.models.json._
import scala.concurrent.Future

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  helpers: Helpers
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    address: Option[String],
    ip: Option[String]
  ) = Action.async { request =>
    Future {
      helpers.getTimezones(address = address, ip = ip) match {
        case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
        case Right(timezones) => Ok(Json.toJson(timezones))
      }
    }
  }

}
