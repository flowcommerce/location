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
  override val controllerComponents: ControllerComponents,
  helpers: Helpers,
  system: ActorSystem
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("addresses-controller-context")

  def get(
    ip: Option[String]
  ) = Action.async { _ =>
    val validatedIp: Either[Seq[String], String] = ip match {
      case None => Left(Seq("Must specify 'ip' parameter"))
      case Some(v) => Right(v)
    }

    validatedIp.left.getOrElse(Nil).toList match {
      case Nil => {
        helpers.getLocations(address = None, ip = ip).map {
          case Left(error) => UnprocessableEntity(Json.toJson(error))
          case Right(locations) => Ok(Json.toJson(locations))
        }
      }
      case errors => Future.successful(
        UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      )
    }
  }

  def postVerifications() = Action.async(parse.json) { request =>
    val address = request.body.as[Address]
    AddressVerifier.toText(address) match {
      case None => {
        Future.successful ( UnprocessableEntity(Json.toJson(Validation.error("Address to verify cannot be empty"))) )
      }

      case Some(text) => {
        helpers.getLocations(address = Some(text)).map {
          case Left(errors) => {
            sys.error(s"Error in address verification: $errors")
          }

          case Right(locations) => {
            Ok(Json.toJson(AddressVerifier(address, locations)))
          }
        }
      }
    }
  }

}
