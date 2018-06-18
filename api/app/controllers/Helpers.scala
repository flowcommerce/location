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
  google: Google,
  @javax.inject.Named("DigitalElementIndex")
  digitalElementIndex: DigitalElementIndex
) {
  def getTimezones(
    address: Option[String] = None,
    ip: Option[String] = None
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Seq[String], Seq[Timezone]]] = {
    getLocations(address = address, ip = ip).flatMap {
      case Left(_) => Future.successful(Left(Seq("Must specify either 'address' or 'ip'")))
      case Right(addresses) => {
        val eithersFuture = Future.sequence(addresses.map { a =>
          (a.latitude, a.longitude) match {
            case (Some(lat), Some(lng)) => google.getTimezone(lat.toDouble, lng.toDouble).map {
              case None => Left("Unable to determine timezone based on address/ip")
              case Some(tz) => Right(tz)
            }
            case _ => Future.successful(Left("Unable to determine latitude/longitude for this address/ip which is required for timezone lookup"))
          }
        })

        eithersFuture.map(eithers => eithers.filter(_.isLeft) match {
          case Nil => Right(eithers.filter(_.isRight).map(_.right.get)) // if there are no errors, then get all the timezones
          case lefts => Left(lefts.map(_.left.get)) // seq of all the errors collected
        })
      }
    }
  }
  
  def getLocations(
    country: Option[String] = None,
    address: Option[String] = None,
    ip: Option[String] = None
  )(
    implicit ec: ExecutionContext
  ): Future[Either[GenericError, Seq[Address]]] = {
    (country, address, ip) match {
      case (Some(code), _, _) => {
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

      case (_, Some(a), _) => {
        // Special case to enable specifying just a country code in the address line
        Countries.find(a) match {
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

          case None => {
            google.getLocationsByAddress(a).map { addrs =>
              Right(addrs)
            }
          }
        }
      }
          
      case (_, _, Some(i)) => Future.successful {
        DigitalElement.ipToDecimal(i) map { ip =>
          digitalElementIndex.lookup(ip).map(_.toAddress).toSeq
        } match {
          case Success(res) => Right(res)
          case Failure(error) => Left(Validation.error(error.getMessage))
        }
      }

      case _ => Future.successful(Left(Validation.error("Must specify either 'address' or 'ip'")))
    }
  }

  def validateIp(ip: Option[String]): Either[Seq[String], Option[DigitalElement.ValidatedIpAddress]] = {
    DigitalElement.validateIp(ip)
  }

  def validateRequiredIp(ip: Option[String]): Either[Seq[String], DigitalElement.ValidatedIpAddress] = {
    DigitalElement.validateIp(ip) flatMap {
      case None => Left(Seq("Must specify 'ip' parameter"))
      case Some(v) => Right(v)
    }
  }
}
