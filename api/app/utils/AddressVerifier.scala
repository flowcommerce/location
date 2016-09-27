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
        streets = compare(Some(address.streets.mkString(" ")), Some(m.streets.mkString(" "))),
        city = compare(address.city, m.city),
        province = compare(address.province, m.province),
        postal = compare(address.postal, m.postal),
        country = compare(
          address.country.flatMap(Countries.find(_).map(_.iso31663)),
          m.country.flatMap(Countries.find(_).map(_.iso31663))
        )
      )
    }
  }

  private[utils] def compare(a: Option[String], b: Option[String]): Boolean = {
    urlKey.format(a.getOrElse("")) == urlKey.format(b.getOrElse(""))
  }

}
