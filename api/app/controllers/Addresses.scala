package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.common.v0.models.Address
import io.flow.common.v0.models.json._
import io.flow.error.v0.models.json._
import io.flow.location.v0.models.json._

import scala.concurrent.Future
import utils.AddressVerifier

@javax.inject.Singleton
class Addresses @javax.inject.Inject() (
  helpers: Helpers,
  system: ActorSystem
) extends Controller {

  private[this] implicit val ec = system.dispatchers.lookup("addresses-controller-context")

  def get(
    address: Option[String],
    ip: Option[String]
  ) = Action.async { request =>
    helpers.getLocations(address = address, ip = ip).map( addrs => addrs match {
      case Left(_) => UnprocessableEntity(Json.toJson(Validation.error("Must specify either 'address' or 'ip'")))
      case Right(locations) => Ok(Json.toJson(locations))
    })
  }

  def postVerifications() = Action.async(parse.json) { request =>
    val address = request.body.as[Address]
    AddressVerifier.toText(address) match {
      case None => {
        Future.successful ( UnprocessableEntity(Json.toJson(Validation.error("Address to verify cannot be empty"))) )
      }

      case Some(text) => {
        helpers.getLocations(address = Some(text)).map ( addrs => addrs match {
          case Left(errors) => {
            sys.error(s"Error in address verification: $errors")
          }

          case Right(locations) => {
            Ok(Json.toJson(AddressVerifier(address, locations)))
          }
        })
      }
    }
  }

}
