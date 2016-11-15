package controllers


import io.flow.common.v0.models.Address
import io.flow.reference.Countries
import io.flow.reference.v0.models.Timezone
import utils._

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class Helpers @javax.inject.Inject() (
  google: Google
) {
  def getTimezones(
    address: Option[String] = None,
    ip: Option[String] = None
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Seq[String], Seq[Timezone]]] = {
    getLocations(address = address, ip = ip).map ( res => res match {
      case Left(_) => Left(Seq("Must specify either 'address' or 'ip'"))
      case Right(addresses) => {
        val eithers = addresses.map{ a =>
          (a.latitude, a.longitude) match {
            case (Some(lat), Some(lng)) => google.getTimezone(lat.toDouble, lng.toDouble) match {
              case None => Left("Unable to determine timezone based on address/ip")
              case Some(tz) => Right(tz)
            }
            case _ => Left("Unable to determine latitude/longitude for this address/ip which is required for timezone lookup")
          }
        }

        eithers.filter(_.isLeft) match {
          case Nil => Right(eithers.filter(_.isRight).map(_.right.get)) // if there are no errors, then get all the timezones
          case lefts => Left(lefts.map(_.left.get)) // seq of all the errors collected
        }
      }
    })
  }
  
  def getLocations(
    country: Option[String] = None,
    address: Option[String] = None,
    ip: Option[String] = None
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Unit, Seq[Address]]] = {
    (country, address, ip) match {
      case (Some(code), _, _) => {
        Countries.find(code) match {
          case None => {
            Future { Right(Nil) }
          }

          case Some(c) => {
            Future {
              Right(
                Seq(
                  Address(
                    country = Some(c.iso31663)
                  )
                )
              )
            }
          }
        }
      }

      case (_, Some(a), _) => {
        // Special case to enable specifying just a country code in the address line
        Countries.find(a) match {
          case Some(c) => {
            Future {
              Right(
                Seq(
                  Address(
                    country = Some(c.iso31663)
                  )
                )
              )
            }
          }

          case None => {
            google.getLocationsByAddress(a).map { addrs =>
              Right(addrs)
            }
          }
        }
      }
          
      case (_, _, Some(i)) => {
        MaxMind.getByIp(i) match {
          case Some(address) => Future { Right(Seq(address)) }
          case None => Future { Right(Nil) }
        }
      }

      case _ => Future { Left(Unit) }
    }
  }
}
