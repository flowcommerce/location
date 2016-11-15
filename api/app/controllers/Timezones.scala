package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.reference.v0.models.json._
import io.flow.error.v0.models.json._

import scala.concurrent.Future

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  helpers: Helpers,
  system: ActorSystem
) extends Controller {

  private[this] implicit val ec = system.dispatchers.lookup("timezones-controller-context")

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
