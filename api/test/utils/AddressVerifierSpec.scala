package utils

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.AddressVerification
import org.scalatestplus.play._

class AddressVerifierSpec extends PlaySpec with OneAppPerSuite {

  val yongeStreet = Address(
    streets = Some(Seq("123 Yonge Street")),
    city = Some("Toronto"),
    province = Some("Ontario"),
    postal = Some("M5C 1W4"),
    country = Some("CAN")
  )

  val ukGardens = Address(
    streets = Some(Seq("76 Belsize Park Gardens")),
    city = Some("London"),
    postal = Some("NW3 4NG"),
    country = Some("UK")
  )

  "compare" in {
    AddressVerifier.compare(None, None) must be(true)
    AddressVerifier.compare(None, Some("a")) must be(false)
    AddressVerifier.compare(Some("a"), None) must be(false)
    AddressVerifier.compare(Some("a"), Some("a")) must be(true)
    AddressVerifier.compare(Some("A"), Some("  a   ")) must be(true)
    AddressVerifier.compare(Some("BHZ 750"), Some("  bhz - 750   ")) must be(true)
  }

  "toSuggestions" in {
    AddressVerifier.toSuggestions(yongeStreet, Nil) must be(Nil)

    val caCopy = yongeStreet.copy(postal = Some("m5c-1w4"), country = Some(" ca "))
    AddressVerifier.toSuggestions(yongeStreet, Seq(caCopy)).toList match {
      case sugg :: Nil => {
        sugg.address must be(caCopy)
        sugg.streets must be(true)
        sugg.city must be(true)
        sugg.province must be(true)
        sugg.postal must be(true)
        sugg.country must be(true)
      }
      case _ => {
        sys.error("Expected exactly one match")
      }
    }

    AddressVerifier.toSuggestions(yongeStreet, Seq(ukGardens)).toList match {
      case sugg :: Nil => {
        sugg.address must be(ukGardens)
        sugg.streets must be(false)
        sugg.city must be(false)
        sugg.province must be(false)
        sugg.postal must be(false)
        sugg.country must be(false)
      }
      case _ => {
        sys.error("Expected exactly one match")
      }
    }
  }

  "apply" in {
    AddressVerifier(yongeStreet, Nil) must be(
      AddressVerification(
        address = yongeStreet,
        valid = false,
        suggestions = Nil
      )
    )

    AddressVerifier(yongeStreet, Seq(ukGardens)).valid must be(false)
    AddressVerifier(yongeStreet, Seq(yongeStreet, ukGardens)).valid must be(true)
  }

}
