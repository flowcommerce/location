package controllers

import akka.actor.ActorSystem
import io.flow.common.v0.models.Address
import io.flow.reference.Countries
import io.flow.reference.v0.models.Timezone
import utils._

@javax.inject.Singleton
class Helpers @javax.inject.Inject() (
  google: Google,
  system: ActorSystem
) {

  private[this] implicit val ec = system.dispatchers.lookup("google-api-context")

  def getTimezones(
    address: Option[String] = None,
    ip: Option[String] = None
  ): Either[Seq[String], Seq[Timezone]] = {
    getLocations(address = address, ip = ip) match {
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
    }
  }
  
  def getLocations(
    country: Option[String] = None,
    address: Option[String] = None,
    ip: Option[String] = None
  ): Either[Unit, Seq[Address]] = {
    (country, address, ip) match {
      case (Some(code), _, _) => {
        Countries.find(code) match {
          case None => {
            Right(Nil)
          }

          case Some(c) => {
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

      case (_, Some(a), _) => {
        // Special case to enable specifying just a country code in the address line
        Countries.find(a) match {
          case Some(c) => {
            Right(
              Seq(
                Address(
                  country = Some(c.iso31663)
                )
              )
            )
          }

          case None => {
            Right(
              google.getLocationsByAddress(a)
            )
          }
        }
      }
          
      case (_, _, Some(i)) => {
        MaxMind.getByIp(i) match {
          case Some(address) => Right(Seq(address))
          case None => Right(Nil)
        }
      }

      case _ => Left(Unit)
    }
  }
}
