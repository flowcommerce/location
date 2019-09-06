package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.common.v0.models.Address
import io.flow.common.v0.models.json._
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.location.v0.models.json._
import io.flow.log.RollbarLogger

import scala.concurrent.Future
import utils.AddressVerifier

@javax.inject.Singleton
class Addresses @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  logger: RollbarLogger,
  helpers: Helpers,
  system: ActorSystem
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("addresses-controller-context")

  def get(
    address: Option[String],
    ip: Option[String]
  ) = Action.async { _ =>
    helpers.getLocations(address = address, ip = ip).map {
      case Left(error) => UnprocessableEntity(Json.toJson(error))
      case Right(locations) => Ok(Json.toJson(locations))
    }
  }

  def postVerifications() = Action.async(parse.json) { request =>
    val address = request.body.as[Address]
    AddressVerifier.toText(address) match {
      case None => {
        Future.successful(UnprocessableEntity(Json.toJson(
          LocationError(
            code = LocationErrorCode.AddressRequired,
            messages = Seq("Address to verify cannot be empty")
          )
        )))
      }

      case Some(text) => {
        helpers.getLocations(address = Some(text)).map {
          case Left(error) => {
            logger
              .withKeyValue("address", text)
              .withKeyValue("error_code", error.code)
              .withKeyValue("error_messages", error.messages)
              .error("Error in address verification")
            sys.error(s"Error in address verification: $error")
          }

          case Right(locations) => {
            Ok(Json.toJson(AddressVerifier(address, locations)))
          }
        }
      }
    }
  }

}
