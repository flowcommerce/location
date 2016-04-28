package controllers

import io.flow.common.v0.models.Healthcheck
import io.flow.common.v0.models.json._
import Helpers
import play.api.mvc._
import play.api.libs.json._

class Healthchecks extends Controller {

  def getHealthcheck() = Action { request =>
    Ok(Json.toJson(Healthcheck("healthy")))
  }

}
