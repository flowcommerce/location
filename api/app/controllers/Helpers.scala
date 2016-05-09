package controllers

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.Location
import io.flow.reference.Countries
import utils._

@javax.inject.Singleton
class Helpers @javax.inject.Inject() (
  google: Google
) {
  def validateRequestParameters(
    address: Option[String],
    latitude: Option[String],
    longitude: Option[String],
    ip: Option[String]
  ): Seq[String] = {
    val validInputError = (ip, address) match {
      case (None, None) => Seq("No valid query string parameters given. Please provide ip or address")
      case _ => Nil
    }

    val latitudeError =
      latitude match {
        case Some (a) => Seq("[latitude] is not yet supported.")
        case None => Nil
      }

    val longitudeError =
      longitude match {
        case Some (a) => Seq("[longitude] is not yet supported.")
        case None => Nil
      }

    validInputError ++ latitudeError ++ longitudeError
  }

  def getLocations(
    address: Option[String],
    latitude: Option[String],
    longitude: Option[String],
    ip: Option[String]
  ): Either[Seq[String], Seq[Location]] = {
    validateRequestParameters(address, latitude, longitude, ip) match {
      case Nil => {
        (ip, address) match {
          case (Some(i), None) => {
            MaxMind.getByIp(i) match {
              case Some(ipl) => MaxMind.getLocation(ipl) match {
                case Left(errors) => Left(errors)
                case Right(location) => Right(Seq(location))
              }
              case None => Left(Seq("No location found for ip [$ip]"))
            }
          }
          case (None, Some(a)) => {
            // Special case to enable specifying just a country code in the address line
            Countries.find(a) match {
              case Some(c) => {
                Right(
                  Seq(
                    Location(
                      address = Address(country = Some(c.iso31663)),
                      latitude = "0",
                      longitude = "0"
                    )
                  )
                )
              }

              case None => {
                Google.getLocationsByAddress(a)
              }
            }
          }
          case _ => Left(Seq("Invalid input. Either use ip or address, not both.")) // limitation for now, we can clean up later
        }
      }
      case errors => {
        Left(errors)
      }
    }
  }
}
