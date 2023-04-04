package controllers

import io.flow.location.v0.errors.{LocationErrorResponse, UnitResponse}
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.test.utils.FlowPlaySpec

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait TestHelpers {
  self: FlowPlaySpec =>

  val UnprocessableEntityStatusCode = 422

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

  def expectUnprocessableEntity[T](
    f: => Future[T]
  ): UnitResponse = {
    Try(await(f)) match {
      case Success(response) => {
        sys.error("Expected function to fail but it succeeded with: " + response)
      }
      case Failure(ex) => ex match {
        case e: UnitResponse => {
          if (e.status != UnprocessableEntityStatusCode) {
            sys.error(s"Expected ${UnprocessableEntityStatusCode} status code but got[${e.status}]")
          }
          e
        }
        case e => {
          sys.error(s"Expected an exception of type[LocationError] but got[$e]")
        }
      }
    }
  }
}
