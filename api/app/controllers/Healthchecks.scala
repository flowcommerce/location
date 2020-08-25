package controllers

import akka.actor.ActorSystem
import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Healthchecks @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  system: ActorSystem,
  environmentVariables: utils.EnvironmentVariables,
  addresses: Addresses
) extends BaseController {

  private[this] implicit val ec = system.dispatchers.lookup("healthchecks-controller-context")

  def getHealthcheck() = Action.async { request =>
    // force loading of config
    assert(environmentVariables.digitalElementFileUri.nonEmpty)

    addresses.get(address = None, ip = Some("0.0.0.0"), countryCode = None, postalCodePrefix = None)(request).map { _ =>
      Ok(Json.toJson(Healthcheck("healthy")))
    }
  }

}
