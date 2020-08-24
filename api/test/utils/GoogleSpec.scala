package utils

import io.flow.reference.data.Countries
import io.flow.test.utils.FlowPlaySpec

class GoogleSpec extends FlowPlaySpec {
  private[this] def google = app.injector.instanceOf[Google]

  "getComponentFilters" in {
    google.getComponentFilters(None) mustBe Nil

    val countryCompFilter = google.getComponentFilters(Some("country:JP")).head
    countryCompFilter.component mustBe "country"
    countryCompFilter.value mustBe Countries.Jpn.iso31662.toUpperCase

    val postalCompFilter = google.getComponentFilters(Some("postal_code_prefix:190")).head
    postalCompFilter.component mustBe "postal_code_prefix"
    postalCompFilter.value mustBe "190"

    val complexCompFilter = google.getComponentFilters(Some("country:JP|postal_code_prefix:190"))
    val firstCompFilter = complexCompFilter.head
    val secondCompFilter = complexCompFilter.reverse.head
    firstCompFilter.component mustBe "country"
    firstCompFilter.value mustBe Countries.Jpn.iso31662.toUpperCase
    secondCompFilter.component mustBe "postal_code_prefix"
    secondCompFilter.value mustBe "190"
  }

}
