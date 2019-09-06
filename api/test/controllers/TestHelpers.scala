package controllers

import io.flow.location.v0.errors.{LocationErrorResponse, UnitResponse}
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.test.utils.FlowPlaySpec
import org.specs2.execute.Result

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait TestHelpers {
  self: FlowPlaySpec =>

  def expectStatus(code: Int)(f: => Future[_]): Result = {
    assert(code >= 400, s"code[$code] must be >= 400")
    await(f) match {
      case Success(_) => {
        org.specs2.execute.Failure(s"Expected HTTP[$code] but got HTTP 2xx")
      }
      case Failure(ex) => ex match {
        case UnitResponse(c) if c == code => {
          org.specs2.execute.Success()
        }
        case UnitResponse(c) => {
          org.specs2.execute.Failure(s"Expected code[$code] but got[$c]")
        }
        case e => {
          org.specs2.execute.Failure(s"Unexpected error: $e")
        }
      }
    }
  }

  def expectErrors[T](code: LocationErrorCode)(
    f: => Future[T]
  ): LocationError = {
    Try(await(f)) match {
      case Success(response) => {
        sys.error("Expected function to fail but it succeeded with: " + response)
      }
      case Failure(ex) =>  ex match {
        case e: LocationErrorResponse => {
          if (e.locationError.code != code) {
            sys.error(s"Expected location error code[$code] but got[${e.locationError.code}]")
          }
          e.locationError
        }
        case e => {
          sys.error(s"Expected an exception of type[LocationError] but got[$e]")
        }
      }
    }
  }
}
