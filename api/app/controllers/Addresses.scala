package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc.{ControllerComponents, _}
import io.flow.common.v0.models.json._
import io.flow.error.v0.models.json._

@javax.inject.Singleton
class Addresses @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  helpers: Helpers,
  system: ActorSystem
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("addresses-controller-context")

  def get(
    ip: Option[String]
  ) = Action.async { _ =>
    helpers.getLocations(ip = ip).map {
      case Left(error) => UnprocessableEntity(Json.toJson(error))
      case Right(locations) => Ok(Json.toJson(locations))
    }
  }

}
