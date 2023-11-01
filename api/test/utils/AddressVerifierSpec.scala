package utils

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.AddressVerification
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class AddressVerifierSpec extends PlaySpec with GuiceOneAppPerSuite {

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

  "toText" in {
    println(s"FOO: " + AddressVerifier.toText(Address()))
    AddressVerifier.toText(Address()) must equal(None)
    AddressVerifier.toText(Address(text = Some("foo"))) must equal(Some("foo"))
    AddressVerifier.toText(yongeStreet) must equal(Some("123 Yonge Street Toronto Ontario M5C 1W4 Canada"))
    AddressVerifier.toText(ukGardens) must equal(Some("76 Belsize Park Gardens London NW3 4NG"))
  }

  "isDifferent" in {
    AddressVerifier.isDifferent(None, None) must equal(false)
    AddressVerifier.isDifferent(None, Some("a")) must equal(true)
    AddressVerifier.isDifferent(Some("a"), None) must equal(true)
    AddressVerifier.isDifferent(Some("a"), Some("a")) must equal(false)
    AddressVerifier.isDifferent(Some("A"), Some("  a   ")) must equal(false)
    AddressVerifier.isDifferent(Some("BHZ 750"), Some("  bhz - 750   ")) must equal(false)
  }

  "toSuggestions" in {
    AddressVerifier.toSuggestions(yongeStreet, Nil) must equal(Nil)

    val caCopy = yongeStreet.copy(postal = Some("m5c-1w4"), country = Some(" ca "))
    AddressVerifier.toSuggestions(yongeStreet, Seq(caCopy)).toList match {
      case sugg :: Nil => {
        sugg.address must equal(caCopy)
        sugg.streets must equal(false)
        sugg.city must equal(false)
        sugg.province must equal(false)
        sugg.postal must equal(false)
        sugg.country must equal(false)
      }
      case _ => {
        sys.error("Expected exactly one match")
      }
    }

    AddressVerifier.toSuggestions(yongeStreet, Seq(ukGardens)).toList match {
      case sugg :: Nil => {
        sugg.address must equal(ukGardens)
        sugg.streets must equal(true)
        sugg.city must equal(true)
        sugg.province must equal(true)
        sugg.postal must equal(true)
        sugg.country must equal(true)
      }
      case _ => {
        sys.error("Expected exactly one match")
      }
    }
  }

  "collapseStreets will collapse leading prefix from google result" in {
    val variant = yongeStreet.copy(
      streets = Some(Seq("123", "Yonge Street"))
    )

    AddressVerifier.collapseStreets(yongeStreet, Seq(variant)) must equal(Seq(yongeStreet))
  }

  "collapseStreets will not merge streets if leading prefix is different" in {
    val variant = yongeStreet.copy(
      streets = Some(Seq("931", "Yonge Street"))
    )

    AddressVerifier.collapseStreets(yongeStreet, Seq(variant)) must equal(Seq(variant))
  }

  "apply" in {
    AddressVerifier(yongeStreet, Nil) must equal(
      AddressVerification(
        address = yongeStreet,
        valid = false,
        suggestions = Nil
      )
    )

    AddressVerifier(yongeStreet, Seq(ukGardens)).valid must equal(false)
    AddressVerifier(yongeStreet, Seq(yongeStreet, ukGardens)).valid must equal(true)
  }

}
