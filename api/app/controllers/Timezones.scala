package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.reference.v0.models.json._
import io.flow.error.v0.models.json._

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
    helpers.getTimezones(address = address, ip = ip).map( tz => tz match {
      case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      case Right(timezones) => Ok(Json.toJson(timezones))
    })
  }

}
