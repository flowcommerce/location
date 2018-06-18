package controllers

import akka.actor.ActorSystem
import io.flow.common.v0.models.json._
import io.flow.error.v0.models.json._
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import io.flow.play.util.Validation
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import utils.{AddressVerifier, Google}

import scala.concurrent.Future

@javax.inject.Singleton
class InternalAddresses @javax.inject.Inject() (
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents,
  system: ActorSystem,
  google: Google
) extends FlowController {
  private[this] implicit val ec = system.dispatchers.lookup("internal-addresses-controller-context")

  def post() = Org.async(parse.json) { request =>
    val address = request.body.as[io.flow.common.v0.models.Address]
    AddressVerifier.toText(address) match {
      case Some(addr) => google.getLocationsByAddress(address = addr) map { results =>
        Ok(Json.toJson(results))
      } recover {
        case ex => {
          val msg = "An unknown error occurred during internal geolocation."
          Logger.warn(s"$msg: ${ex.getMessage}")
          UnprocessableEntity(Json.toJson(Validation.error(msg)))
        }
      }
      case None => Future.successful {
        UnprocessableEntity(Json.toJson(Validation.error(
          "Address to verify cannot be empty"
        )))
      }
    }

  }
}
