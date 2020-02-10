package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext
import play.api.mvc.{AnyContent, ControllerComponents, Request}

import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.controllers.HealthchecksController

@Singleton
class HealthchecksEC @Inject() (system: ActorSystem)
   extends CustomExecutionContext(system, "healthchecks-controller-context")

@Singleton
class Healthchecks @Inject() (
  environmentVariables: utils.EnvironmentVariables,
  addresses: Addresses,
  val controllerComponents: ControllerComponents,
)(implicit ec: HealthchecksEC) extends HealthchecksController {

  def getHealthcheck(req: Request[AnyContent]) = {
    // force loading of config
    assert(environmentVariables.digitalElementFileUri.nonEmpty)

    addresses.get(req, address = None, ip = Some("0.0.0.0"))
      .map(_ => GetHealthcheck.HTTP200(Healthcheck("healthy")))
  }

}
