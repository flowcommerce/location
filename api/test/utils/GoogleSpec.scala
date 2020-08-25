package utils

import io.flow.reference.data.Countries
import io.flow.test.utils.FlowPlaySpec

class GoogleSpec extends FlowPlaySpec {
  private[this] def google = app.injector.instanceOf[Google]

  "getComponentFilters" in {
    google.getComponentFilters(None, None) mustBe Nil
    google.getComponentFilters(Some("invalid"), None) mustBe Nil

    val countryiso2CompFilter = google.getComponentFilters(Some(Countries.Jpn.iso31662), None).head
    countryiso2CompFilter.component mustBe "country"
    countryiso2CompFilter.value mustBe Countries.Jpn.iso31662.toUpperCase

    val countryiso3CompFilter = google.getComponentFilters(Some(Countries.Jpn.iso31663), None).head
    countryiso3CompFilter.component mustBe "country"
    countryiso3CompFilter.value mustBe Countries.Jpn.iso31662.toUpperCase

    val countryNameCompFilter = google.getComponentFilters(Some(Countries.Jpn.name), None).head
    countryNameCompFilter.component mustBe "country"
    countryNameCompFilter.value mustBe Countries.Jpn.iso31662.toUpperCase

    val postalCompFilter = google.getComponentFilters(None, Some("190")).head
    postalCompFilter.component mustBe "postal_code_prefix"
    postalCompFilter.value mustBe "190"

    val complexCompFilter = google.getComponentFilters(Some(Countries.Jpn.iso31662),Some("190"))
    val firstCompFilter = complexCompFilter.head
    val secondCompFilter = complexCompFilter.reverse.head
    firstCompFilter.component mustBe "country"
    firstCompFilter.value mustBe Countries.Jpn.iso31662.toUpperCase
    secondCompFilter.component mustBe "postal_code_prefix"
    secondCompFilter.value mustBe "190"
  }

}
