package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Named, Singleton}
import play.api.libs.concurrent.CustomExecutionContext
import play.api.mvc.{AnyContent, ControllerComponents, Request}
import scala.concurrent.Future

import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.location.v0.controllers.TimezonesController
import utils.DigitalElementIndex

@Singleton
class TimezonesEC @Inject() (system: ActorSystem)
   extends CustomExecutionContext(system, "timezones-controller-context")

@Singleton
class Timezones @Inject() (
  @Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex,
  helpers: Helpers,
  val controllerComponents: ControllerComponents,
)(implicit ec: TimezonesEC) extends TimezonesController {

  def get(req: Request[AnyContent], ip: Option[String]) = Future {
    helpers.validateRequiredIp(ip)
      .fold(
        Get.HTTP422,
        valid =>
          digitalElementIndex.lookup(valid)
            .flatMap(_.timezone)
            .fold(getTimezoneUnavailable(ip.getOrElse("")))(tz => Get.HTTP200(Seq(tz)))
      )
  }

  private def getTimezoneUnavailable(ip: String): Get = {
    val error = LocationError(
      code = LocationErrorCode.TimezoneUnavailable,
      messages = Seq(s"Timezone information not available for ip '${ip.trim}'")
    )

    Get.HTTP422(error)
  }

}
