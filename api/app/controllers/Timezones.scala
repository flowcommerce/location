package controllers

import akka.actor.ActorSystem
import io.flow.location.v0.models.json._
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.reference.v0.models.json._
import io.flow.reference.{Countries, Timezones}
import play.api.libs.json._
import play.api.mvc._
import utils.Ip2Location

import scala.concurrent.Future

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  @javax.inject.Named("Ip2LocationIndex") ip2Location: IndexedSeq[Ip2Location],
  helpers: Helpers,
  system: ActorSystem,
) extends BaseController {
  private[this] implicit val ec: akka.dispatch.MessageDispatcher = system.dispatchers.lookup("controller-context")

  def get(
    ip: Option[String],
  ) = Action.async { _ =>
    Future {
      helpers.validateRequiredIp(ip) match {
        case Left(error) => {
          UnprocessableEntity(Json.toJson(error))
        }
        case Right(valid) =>
          {
            for {
              location <- ip2Location.lookup(valid.intValue)
              country <- location.toAddress.country
              timezone <- Countries.find(country).map(_.timezones)
            } yield timezone
          } match {
            case Some(tzs) =>
              val timezones = tzs.map(Timezones.find)
              if (timezones.isEmpty) {
                UnprocessableEntity(
                  Json.toJson(
                    LocationError(
                      code = LocationErrorCode.TimezoneUnavailable,
                      messages = Seq(
                        s"Timezone information not available for ip '${ip.get.trim}'",
                      ),
                    ),
                  ),
                )
              } else Ok(Json.toJson(timezones))
            case None => {
              UnprocessableEntity(
                Json.toJson(
                  LocationError(
                    code = LocationErrorCode.TimezoneUnavailable,
                    messages = Seq(
                      s"Timezone information not available for ip '${ip.get.trim}'",
                    ),
                  ),
                ),
              )
            }
          }
      }
    }
  }

}
