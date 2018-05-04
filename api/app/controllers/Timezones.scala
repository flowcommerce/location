package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.error.v0.models.json._

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  helpers: Helpers,
  system: ActorSystem
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("timezones-controller-context")

  def get(
    address: Option[String],
    ip: Option[String]
  ) = Action { _ =>
    UnprocessableEntity(Json.toJson(Validation.error("timezone lookup not available")))
  }

}
