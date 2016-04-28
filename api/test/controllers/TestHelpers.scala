package controllers

import java.util.concurrent.TimeUnit
import io.flow.location.v0.errors.UnitResponse
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait TestHelpers {

  val DefaultDuration = Duration(5, TimeUnit.SECONDS)

  def expectStatus(code: Int)(f: => Unit) {
    assert(code >= 400, s"code[$code] must be >= 400")

    Try(
      f
    ) match {
      case Success(response) => {
        org.specs2.execute.Failure(s"Expected HTTP[$code] but got HTTP 2xx")
      }
      case Failure(ex) => ex match {
        case UnitResponse(code) => {
          org.specs2.execute.Success()
        }
        case e => {
          org.specs2.execute.Failure(s"Unexpected error: $e")
        }
      }
    }
  }
}

