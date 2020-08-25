package controllers

import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.models.json._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@javax.inject.Singleton
class Healthchecks @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  environmentVariables: utils.EnvironmentVariables,
  addresses: Addresses
)(implicit ec: ExecutionContext) extends BaseController {

  def getHealthcheck() = Action.async { request =>
    // force loading of config
    assert(environmentVariables.digitalElementFileUri.nonEmpty)

    addresses.get(address = None, ip = Some("0.0.0.0"), country = None, postalPrefix = None)(request).map { _ =>
      Ok(Json.toJson(Healthcheck("healthy")))
    }
  }

}
