package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import scala.concurrent.{ExecutionContext, Future}

import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.location.v0.controllers.TimezonesController
import utils.DigitalElementIndex

@Singleton
class Timezones @Inject() (
  @Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex,
  system: ActorSystem,
  helpers: Helpers,
  cc: ControllerComponents,
) extends AbstractController(cc) with TimezonesController {

  private[this] implicit val ec: ExecutionContext = system.dispatchers.lookup("timezones-controller-context")

  def get(req: Request[AnyContent], ip: Option[String]) = Future {
    val locationError = LocationError(
      code = LocationErrorCode.TimezoneUnavailable,
      messages = ip.map(ip => s"Timezone information not available for ip '${ip.trim}'").toList
    )

    helpers.validateRequiredIp(ip)
      .fold(
        error => Get.HTTP422(error),
        valid =>
          digitalElementIndex.lookup(valid)
            .flatMap(_.timezone)
            .fold[Get](Get.HTTP422(locationError))(tz => Get.HTTP200(Seq(tz)))
      )
  }

}
