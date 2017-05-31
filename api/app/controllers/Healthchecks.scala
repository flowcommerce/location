package controllers

import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

@javax.inject.Singleton
class Healthchecks @javax.inject.Inject() (
  environmentVariables: utils.EnvironmentVariables, addresses: Addresses
) extends Controller {

  private[this] implicit val ec = ExecutionContext.global

  def getHealthcheck() = Action.async { request =>
    addresses.get(None, Some("0.0.0.0"))(request) map { _ =>
      Ok(Json.toJson(Healthcheck("healthy")))
    }
  }

}
