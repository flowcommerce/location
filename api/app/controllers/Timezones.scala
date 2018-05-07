package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.error.v0.models.json._
import io.flow.reference.v0.models.json._
import utils.DigitalElementIndex

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  @javax.inject.Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex,
  system: ActorSystem,
  helpers: Helpers
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("timezones-controller-context")

  def get(
    ip: Option[String]
  ) = Action { _ =>
    val validatedIp = helpers.validateRequiredIp(ip)

    validatedIp.left.getOrElse(Nil).toList match {
      case Nil => {
        digitalElementIndex.lookup(validatedIp.right.get).flatMap (_.timezone) match {
          case None => {
            UnprocessableEntity(Json.toJson(Validation.error(
              s"Timezone information not available for ip '${ip.get.trim}'"
            )))
          }

          case Some(tz) => {
            Ok(Json.toJson(Seq(tz)))
          }
        }
      }

      case errors => {
        UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      }
    }
  }

}
