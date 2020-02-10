package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext
import play.api.mvc.{AnyContent, ControllerComponents, Request}
import scala.concurrent.Future

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.location.v0.models.json._
import io.flow.location.v0.controllers.AddressesController
import io.flow.log.RollbarLogger

import utils.AddressVerifier

@Singleton
class AddressesEC @Inject() (system: ActorSystem)
   extends CustomExecutionContext(system, "addresses-controller-context")

@Singleton
class Addresses @Inject() (
  logger: RollbarLogger,
  helpers: Helpers,
  val controllerComponents: ControllerComponents,
)(implicit ec: AddressesEC) extends AddressesController {

  def get(req: Request[AnyContent], address: Option[String], ip: Option[String]) =
    helpers.getLocations(address = address, ip = ip)
      .map(_.fold(Get.HTTP422, Get.HTTP200))

  def postVerifications(req: Request[Address], body: Address) =
    AddressVerifier.toText(body)
      .fold(Future.successful(postVerificationsEmptyAddress)) { text =>
        helpers.getLocations(address = Some(text)).map {
          _.fold(
            postVerificationsBadAddress(text, _),
            locations => PostVerifications.HTTP200(AddressVerifier(body, locations)))
        }
      }

  private lazy val postVerificationsEmptyAddress: PostVerifications = {
    val error = LocationError(
      code = LocationErrorCode.AddressRequired,
      messages = Seq("Address to verify cannot be empty")
    )

    PostVerifications.HTTP422(error)
  }

  private def postVerificationsBadAddress(text: String, error: LocationError): PostVerifications = {
    logger
      .withKeyValue("address", text)
      .withKeyValue("error_code", error.code)
      .withKeyValue("error_messages", error.messages)
      .error("Error in address verification")

    PostVerifications.Undocumented(InternalServerError(s"Error in address verification: $error"))
  }

}
