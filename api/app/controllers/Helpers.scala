package controllers

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.reference.Countries
import io.flow.reference.v0.models.Timezone
import utils.{DigitalElement, Google, Ip2Location, IpUtil, ValidatedIpAddress}

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class Helpers @javax.inject.Inject() (
  google: Google,
  @javax.inject.Named("Ip2LocationIndex") ip2Location: IndexedSeq[Ip2Location],
) {
  def getTimezones(
    address: Option[String] = None,
    ip: Option[String] = None,
  )(implicit
    ec: ExecutionContext,
  ): Future[Either[Seq[String], Seq[Timezone]]] = {
    getLocations(address = address, ip = ip).flatMap {
      case Left(_) => Future.successful(Left(Seq("Must specify either 'address' or 'ip'")))
      case Right(addresses) => {
        val eithersFuture = Future.sequence(addresses.map { a =>
          (a.latitude, a.longitude) match {
            case (Some(lat), Some(lng)) =>
              google.getTimezone(lat.toDouble, lng.toDouble).map {
                case None => Left("Unable to determine timezone based on address/ip")
                case Some(tz) => Right(tz)
              }
            case _ =>
              Future.successful(
                Left("Unable to determine latitude/longitude for this address/ip which is required for timezone lookup"),
              )
          }
        })

        eithersFuture.map(eithers =>
          eithers.collect { case Left(e) => e } match {
            case Nil =>
              Right(eithers.collect { case Right(timezone) =>
                timezone
              }) // if there are no errors, then get all the timezones
            case errors => Left(errors)
          },
        )
      }
    }
  }

  def getLocations(
    country: Option[String] = None,
    address: Option[String] = None,
    ip: Option[String] = None,
    countryParam: Option[String] = None,
    postalPrefix: Option[String] = None,
  )(implicit
    ec: ExecutionContext,
  ): Future[Either[LocationError, Seq[Address]]] = {
    (country, address, ip) match {
      case (Some(code), _, _) => {
        Countries.find(code) match {
          case None => Future.successful(Right(Nil))
          case Some(c) => Future.successful(Right(Seq(Address(country = Some(c.iso31663)))))
        }
      }

      case (_, Some(a), _) => {
        // Special case to enable specifying just a country code in the address line
        Countries.find(a) match {
          case Some(c) => Future.successful(Right(Seq(Address(country = Some(c.iso31663)))))
          case None => google.getLocationsByAddress(a, countryParam, postalPrefix).map(Right.apply)
        }
      }

      case (_, _, Some(i)) =>
        Future.successful {
          println(s"11111111111111111111")
          IpUtil.ipToDecimal(i) map { ip =>
            println(s"2222222222   $ip")
            val res = ip2Location.lookup(ip)
            println(s"3333333333333333 $res")
            res.map(_.toAddress).toSeq
          }
        }

      case _ =>
        Future.successful(
          Left(
            LocationError(
              code = LocationErrorCode.IpRequired,
              messages = Seq("Must specify either 'address' or 'ip'"),
            ),
          ),
        )
    }
  }

  def validateIp(ip: Option[String]): Either[LocationError, Option[DigitalElement.ValidatedIpAddress]] = {
    DigitalElement.validateIp(ip)
  }

  def validateRequiredIp(ip: Option[String]): Either[LocationError, ValidatedIpAddress] = {
    IpUtil.validateIp(ip) match {
      case Left(error) => Left(error)
      case Right(None) =>
        Left(
          LocationError(
            code = LocationErrorCode.IpRequired,
            messages = Seq("Must specify 'ip' parameter"),
          ),
        )
      case Right(Some(v)) => Right(v)
    }
  }
}
