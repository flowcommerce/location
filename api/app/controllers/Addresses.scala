package controllers

import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.common.v0.models.Address
import io.flow.common.v0.models.json._
import io.flow.location.v0.models.{AddressVerification, AddressSuggestion}
import io.flow.location.v0.models.json._
import scala.concurrent.Future

@javax.inject.Singleton
class Addresses @javax.inject.Inject() (
  helpers: Helpers
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    address: Option[String],
    ip: Option[String]
  ) = Action.async { request =>
    Future {
      helpers.getLocations(address = address, ip = ip) match {
        case Left(_) => UnprocessableEntity(Json.toJson(Validation.error("Must specify either 'address' or 'ip'")))
        case Right(locations) => Ok(Json.toJson(locations))
      }
    }
  }

  def postVerifications() = Action.async(parse.json) { request =>
    val address = request.body.as[Address]
    Future {
      Ok(
        AddressVerification(
          address = address,
          valid = true,
          suggestions = Nil
        )
      )
      /*
      helpers.getLocations(address = address) match {
        case Left(errors) => sys.error(s"Error in address verification: $errors")
        case Right(locations) => {
          // TODO: Diff locations
        }
      }
       */
    }
  }

}
