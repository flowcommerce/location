package controllers

import akka.actor.ActorSystem
import play.api.libs.json._
import play.api.mvc._
import io.flow.play.util.Validation
import io.flow.error.v0.models.json._
import io.flow.reference.v0.models.json._
import utils.{DigitalElement, DigitalElementIndex}

import scala.util.{Failure, Success}

@javax.inject.Singleton
class Timezones @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  @javax.inject.Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex,
  system: ActorSystem
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("timezones-controller-context")

  def get(
    ip: Option[String]
  ) = Action { _ =>
    val validatedIp: Either[Seq[String], BigInt] = ip match {
      case None => Left(Seq("Must specify 'ip' parameter"))
      case Some(v) => DigitalElement.ipToDecimal(v) match {
        case Success(valid) => Right(valid)
        case Failure(_) => Left(Seq("Invalid ip address '$v'"))
      }
    }

    validatedIp.left.getOrElse(Nil).toList match {
      case Nil => {
        digitalElementIndex.lookup(validatedIp.right.get).flatMap (_.timezone) match {
          case None => {
            UnprocessableEntity(Json.toJson(Validation.error(
              s"Timezone information not available for ip '$ip'"
            )))
          }

          case Some(tz) => {
            Ok(Json.toJson(tz))
          }
        }
      }

      case errors => {
        UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      }
    }
  }

}
