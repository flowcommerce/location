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
    println(request.body)
    Future {
      val address = request.body.as[Address]
      println(address)
      val text = address.text.getOrElse {
        (address.streets ++ Seq(address.city, address.province, address.postal, address.country).flatten).mkString(", ")
      }
      text.trim match {
        case "" => {
          UnprocessableEntity(Json.toJson(Validation.error("Address to verify cannot be empty")))
        }

        case value => {
          helpers.getLocations(address = Some(text)) match {
            case Left(errors) => sys.error(s"Error in address verification: $errors")
            case Right(locations) => {
              val isValid = locations.toList match {
                case Nil => {
                  // Assume good
                  true
                }

                case loc :: rest => {
                  // Match only on country now
                  (address.country.isEmpty || address.country == loc.country)
                }
              }
              Ok(
                Json.toJson(
                  AddressVerification(
                    address = address,
                    valid = isValid,
                    suggestions = toSuggestions(address, locations)
                  )
                )
              )
            }
          }
        }
      }
    }
  }

  private[this] def toSuggestions(address: Address, locations: Seq[Address]): Seq[AddressSuggestion] = {
    Nil
  }
}
