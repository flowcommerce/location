package utils

import io.flow.play.util.UrlKey
import io.flow.common.v0.models.Address
import io.flow.location.v0.models.{AddressVerification, AddressSuggestion}
import io.flow.reference.Countries

object AddressVerifier {

  private[this] val urlKey = UrlKey(minKeyLength = 1)

  /**
    * Given an address and list of suggested matches, transforms into
    * an AddressVerification object
    */
  def apply(address: Address, matched: Seq[Address]): AddressVerification = {
    val isValid = matched.toList match {
      case Nil => {
        // No suggestions from google; address could not be
        // parsed
        false
      }

      case first :: rest => {
        // For now, all we do is match on country. Later we plan to
        // add actual address verification.
        (address.country, first.country) match {
          case (Some(a), Some(b)) => Countries.find(a) == Countries.find(b)
          case (_, _) => true
        }
      }
    }

    AddressVerification(
      address = address,
      valid = isValid,
      suggestions = toSuggestions(address, matched)
    )
  }

  private[utils] def toSuggestions(address: Address, matched: Seq[Address]): Seq[AddressSuggestion] = {
    matched.map { m =>
      AddressSuggestion(
        address = m,
        streets = isDifferent(Some(address.streets.mkString(" ")), Some(m.streets.mkString(" "))),
        city = isDifferent(address.city, m.city),
        province = isDifferent(address.province, m.province),
        postal = isDifferent(address.postal, m.postal),
        country = isDifferent(
          address.country.flatMap(Countries.find(_).map(_.iso31663)),
          m.country.flatMap(Countries.find(_).map(_.iso31663))
        )
      )
    }
  }

  private[utils] def isDifferent(a: Option[String], b: Option[String]): Boolean = {
    urlKey.format(a.getOrElse("")) != urlKey.format(b.getOrElse(""))
  }

  def toText(address: Address): Option[String] = {
    val text = address.text.getOrElse {
      Seq(
        address.streets.map { values =>
          values.mkString(" ")
        },
        address.city,
        address.province,
        address.postal,
        address.country.flatMap { Countries.find(_).map(_.name) }
      ).flatten.mkString(" ")
    }

    text.trim match {
      case "" => None
      case v => Some(v)
    }
  }
  
}
