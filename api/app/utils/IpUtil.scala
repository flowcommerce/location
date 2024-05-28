package utils

import io.flow.location.v0.models.{LocationError, LocationErrorCode}

import scala.util.{Failure, Success, Try}

case class ValidatedIpAddress(ip: String, intValue: BigInt)

object IpUtil {

  private[this] val ipv4 = "(?:::ffff:)?(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)".r
  private[this] val ipv6 = "([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*)((:[a-fA-F0-9]*)*)".r

  private[this] val IpV4Byte1 = scala.math.pow(256, 3).toLong
  private[this] val IpV4Byte2 = scala.math.pow(256, 2).toLong
  private[this] val IpV4Byte3 = scala.math.pow(256, 1).toLong
  private[this] val IpV4Byte4 = scala.math.pow(256, 0).toLong

  private[this] val IpV6Byte1 = scala.math.pow(65536, 3).toLong
  private[this] val IpV6Byte2 = scala.math.pow(65536, 2).toLong
  private[this] val IpV6Byte3 = scala.math.pow(65536, 1).toLong
  private[this] val IpV6Byte4 = scala.math.pow(65536, 0).toLong

  private[this] def z(s: String) = s match {
    case "" => "0"
    case _ => s
  }

  def ipToDecimal(ip: String): Either[LocationError, BigInt] = {
    Try {
      ip match {
        case ipv4(a, b, c, d) => {
          a.toInt * IpV4Byte1 +
            b.toInt * IpV4Byte2 +
            c.toInt * IpV4Byte3 +
            d.toInt * IpV4Byte4
        }
        case ipv6(a, b, c, d, _*) => {
          Integer.parseInt(z(a), 16) * IpV6Byte1 +
            Integer.parseInt(z(b), 16) * IpV6Byte2 +
            Integer.parseInt(z(c), 16) * IpV6Byte3 +
            Integer.parseInt(z(d), 16) * IpV6Byte4
        }
        case _ => throw new IllegalArgumentException(s"Unable to parse ip address ${ip}")
      }
    } match {
      case Success(r) => Right(r)
      case Failure(_) =>
        Left(
          LocationError(
            code = LocationErrorCode.IpInvalid,
            messages = Seq(s"Unable to parse ip address ${ip}"),
          ),
        )
    }
  }

  def validateIp(ip: Option[String]): Either[LocationError, Option[ValidatedIpAddress]] = {
    ip.map(_.trim).filter(_.nonEmpty) match {
      case None => Right(None)
      case Some(v) =>
        ipToDecimal(v).map { valid =>
          Some(ValidatedIpAddress(v, valid))
        }
    }
  }

}
