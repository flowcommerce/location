package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.controllers.HealthchecksController

@Singleton
class Healthchecks @Inject() (
  system: ActorSystem,
  environmentVariables: utils.EnvironmentVariables,
  addresses: Addresses,
  cc: ControllerComponents,
) extends AbstractController(cc) with HealthchecksController {

  private[this] implicit val ec = system.dispatchers.lookup("healthchecks-controller-context")

  def getHealthcheck(req: Request[AnyContent]) = {
    // force loading of config
    assert(environmentVariables.digitalElementFileUri.nonEmpty)

    addresses.get(req, address = None, ip = Some("0.0.0.0"))
      .map(_ => GetHealthcheck.HTTP200(Healthcheck("healthy")))
  }

}
