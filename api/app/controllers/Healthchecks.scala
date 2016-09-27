package controllers

import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Healthchecks @javax.inject.Inject() (
  environmentVariables: utils.EnvironmentVariables
) extends Controller {

  def getHealthcheck() = Action { request =>
    Ok(Json.toJson(Healthcheck("healthy")))
  }

}
