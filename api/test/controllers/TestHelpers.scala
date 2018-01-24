package controllers

import io.flow.location.v0.errors.{GenericErrorResponse, UnitResponse}
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait TestHelpers {

  val DefaultDuration = Duration(1, TimeUnit.SECONDS)

  def expectStatus[T](code: Int)(f: => Future[T]) {
    assert(code >= 400, s"code[$code] must be >= 400")

    Try(
      Await.result(f, DefaultDuration)
    ) match {
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

  def expectErrors[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ): GenericErrorResponse = {
    Try(
      Await.result(f, duration)
    ) match {
      case Success(response) => {
        sys.error("Expected function to fail but it succeeded with: " + response)
      }
      case Failure(ex) =>  ex match {
        case e: GenericErrorResponse => {
          e
        }
        case e => {
          sys.error(s"Expected an exception of type[GenericErrorResponse] but got[$e]")
        }
      }
    }
  }
}
