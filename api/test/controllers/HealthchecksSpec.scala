package controllers

import io.flow.healthcheck.v0.Client
import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.test.utils.FlowPlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class HealthchecksSpec extends FlowPlaySpec
  with GuiceOneServerPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout
{

  import scala.concurrent.ExecutionContext.Implicits.global

  def client = new Client(wsClient, s"http://localhost:$port")

  "GET /_internal_/healthcheck" in {
    await(
      client.healthchecks.getHealthcheck()
    ) must equal(
      Healthcheck("healthy")
    )
  }

}
