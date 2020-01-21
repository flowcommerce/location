package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import scala.concurrent.Future

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.location.v0.models.json._
import io.flow.location.v0.controllers.AddressesController
import io.flow.log.RollbarLogger

import utils.AddressVerifier

@Singleton
class Addresses @Inject() (
  logger: RollbarLogger,
  helpers: Helpers,
  system: ActorSystem,
  cc: ControllerComponents,
) extends AbstractController(cc) with AddressesController {

  private[this] implicit val ec = system.dispatchers.lookup("addresses-controller-context")

  def get(
    req: Request[AnyContent],
    address: Option[String],
    ip: Option[String]
  ) = helpers.getLocations(address = address, ip = ip)
        .map {
          case Left(error) => Get.HTTP422(error)
          case Right(locations) => Get.HTTP200(locations)
      }

  def postVerifications(req: Request[Address], body: Address) =
    AddressVerifier.toText(body) match {
      case None =>
        val error = LocationError(
            code = LocationErrorCode.AddressRequired,
            messages = Seq("Address to verify cannot be empty")
          )

        Future.successful(PostVerifications.HTTP422(error))

      case Some(text) =>
        helpers.getLocations(address = Some(text)).map {
          case Left(error) =>
            logger
              .withKeyValue("address", text)
              .withKeyValue("error_code", error.code)
              .withKeyValue("error_messages", error.messages)
              .error("Error in address verification")

            sys.error(s"Error in address verification: $error")


          case Right(locations) =>
            PostVerifications.HTTP200(AddressVerifier(body, locations))
          }
    }
}
