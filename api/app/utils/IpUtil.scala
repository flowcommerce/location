package utils

import io.flow.location.v0.models.{LocationError, LocationErrorCode}

import scala.util.{Failure, Success, Try}

case class ValidatedIpAddress(ip: String, intValue: BigInt)

object IpUtil {

  private[this] val ipv4 = "(?:::ffff:)?(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)".r
  // private[this] val ipv6 = "([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*)((:[a-fA-F0-9]*)*)".r
  private[this] val ipv6a =
    "([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*)".r

  private[this] val IpV4Byte1 = scala.math.pow(256, 3).toLong
  private[this] val IpV4Byte2 = scala.math.pow(256, 2).toLong
  private[this] val IpV4Byte3 = scala.math.pow(256, 1).toLong
  private[this] val IpV4Byte4 = scala.math.pow(256, 0).toLong

  /* private[this] val IpV6Byte1 = scala.math.pow(65536, 3).toLong
  private[this] val IpV6Byte2 = scala.math.pow(65536, 2).toLong
  private[this] val IpV6Byte3 = scala.math.pow(65536, 1).toLong
  private[this] val IpV6Byte4 = scala.math.pow(65536, 0).toLong*/

  private[this] val IpV6Byte1a = BigInt(2).pow(16 * 7)
  private[this] val IpV6Byte2a = BigInt(2).pow(16 * 6)
  private[this] val IpV6Byte3a = BigInt(2).pow(16 * 5)
  private[this] val IpV6Byte4a = BigInt(2).pow(16 * 4)
  private[this] val IpV6Byte5a = BigInt(2).pow(16 * 3)
  private[this] val IpV6Byte6a = BigInt(2).pow(16 * 2)
  private[this] val IpV6Byte7a = BigInt(2).pow(16 * 1)
  private[this] val IpV6Byte8a = BigInt(2).pow(16 * 0)

  private[this] def z(s: String) = s match {
    case "" => "0"
    case _ => s
  }

  private def isValidIpv6Address(address: String): Boolean = {
    address.contains(":")
  }

  def expandIfIPv6Address(address: String): String = {
    if (isValidIpv6Address(address)) {
      val parts = address.split("::", 2)
      val leftPart =
        if (parts.head.nonEmpty) parts.headOption.map(_.split(":")).toArrayCustom
        else Array.empty[String]
      val rightPart =
        if (parts.length > 1) parts(1).split(":") else Array.empty[String]
      val numZeroGroups = 8 - (leftPart.length + rightPart.length)

      val fullAddress = (leftPart ++ Array.fill(numZeroGroups)("0000") ++ rightPart)
        .map(part => f"${Integer.parseInt(part, 16)}%04x")
        .mkString(":")

      fullAddress
    } else address
  }

  def ipToDecimal(ip: String): Either[LocationError, BigInt] = {
    Try {
      expandIfIPv6Address(ip) match {
        case ipv4(a, b, c, d) => {
          val res = a.toInt * IpV4Byte1 +
            b.toInt * IpV4Byte2 +
            c.toInt * IpV4Byte3 +
            d.toInt * IpV4Byte4

          BigInt(res)
        }

        case ipv6a(a, b, c, d, e, f, g, h) => {
          BigInt(z(a), 16) * IpV6Byte1a +
            BigInt(z(b), 16) * IpV6Byte2a +
            BigInt(z(c), 16) * IpV6Byte3a +
            BigInt(z(d), 16) * IpV6Byte4a +
            BigInt(z(e), 16) * IpV6Byte5a +
            BigInt(z(f), 16) * IpV6Byte6a +
            BigInt(z(g), 16) * IpV6Byte7a +
            BigInt(z(h), 16) * IpV6Byte8a
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
