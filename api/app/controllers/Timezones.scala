package controllers

import io.flow.location.v0.models.json._
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.reference.v0.models.json._
import play.api.libs.json._
import play.api.mvc._
import utils.DigitalElementIndex

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  @javax.inject.Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex,
  helpers: Helpers
) extends BaseController {

  def get(
    ip: Option[String]
  ) = Action { _ =>
      helpers.validateRequiredIp(ip) match {
        case Left(error) => {
          UnprocessableEntity(Json.toJson(error))
        }
        case Right(valid) =>
          digitalElementIndex.lookup(valid).flatMap(_.timezone) match {
            case None => {
              UnprocessableEntity(Json.toJson(
                LocationError(
                  code = LocationErrorCode.TimezoneUnavailable,
                  messages = Seq(
                    s"Timezone information not available for ip '${ip.get.trim}'"
                  )
                )
              ))
            }

            case Some(tz) => {
              Ok(Json.toJson(Seq(tz)))
            }
          }
      }
  }

}
