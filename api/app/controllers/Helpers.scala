package controllers

import io.flow.common.v0.models.Address
import io.flow.error.v0.models.GenericError
import io.flow.play.util.Validation
import io.flow.reference.Countries
import io.flow.reference.v0.models.Timezone
import utils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@javax.inject.Singleton
class Helpers @javax.inject.Inject() (
  @javax.inject.Named("DigitalElementIndex") digitalElementIndex: DigitalElementIndex
) {

  def getLocations(
    country: Option[String] = None,
    ip: Option[String] = None
  )(
    implicit ec: ExecutionContext
  ): Future[Either[GenericError, Seq[Address]]] = {
    (country, ip) match {
      case (Some(code), _) => {
        Countries.find(code) match {
          case None => {
            Future.successful ( Right(Nil) )
          }

          case Some(c) => {
            Future.successful (
              Right(
                Seq(
                  Address(
                    country = Some(c.iso31663)
                  )
                )
              )
            )
          }
        }
      }

      case (_, Some(i)) => {
        DigitalElement.ipToDecimal(i) map { ip =>
          digitalElementIndex.lookup(ip) map (_.toAddress()) match {
            case Some(address) => Seq(address)
            case None => Nil
          }
        } match {
          case Success(res) => Future.successful(Right(res))
          case Failure(error) => Future.successful(Left(Validation.error(error.getMessage())))
        }
      }

      case _ => Future.successful (Left(Validation.error("Must specify either 'address' or 'ip'")) )
    }
  }
}
