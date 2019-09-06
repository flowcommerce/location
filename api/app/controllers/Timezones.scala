package controllers

import akka.actor.ActorSystem
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import play.api.libs.json._
import play.api.mvc._
import io.flow.location.v0.models.json._
import io.flow.reference.v0.models.json._
import utils.DigitalElementIndex

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  @javax.inject.Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex,
  system: ActorSystem,
  helpers: Helpers
) extends BaseController {

  private[this] implicit val ec: ExecutionContext = system.dispatchers.lookup("timezones-controller-context")

  def get(
    ip: Option[String]
  ) = Action.async { _ =>
    Future {
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

}
