package utils

import com.google.common.collect.{TreeRangeMap, Range => GuavaRange}
import io.flow.common.v0.models.Address
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import io.flow.reference.data.Countries
import io.flow.reference.v0.models.Country
import org.scalatest.{EitherValues, Matchers, OptionValues, WordSpec}
import utils.DigitalElement.ValidatedIpAddress

class DigitalElementSpec extends WordSpec with Matchers with OptionValues with EitherValues {

  // convenience to build DigitalElementIndexRecord fixtures with defaults
  private def indexRecordFactory(
    rangeStart: BigInt = Long.MinValue,
    rangeEnd: BigInt = Long.MaxValue,
    country: Country,
    region: String = "",
    city: String = "",
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    postalCode: String = DigitalElement.PlaceholderPostal
  ) = DigitalElementIndexRecord(
    bytes = Seq(rangeStart, rangeEnd, country.iso31663, region, city, latitude, longitude, postalCode)
      .mkString(DigitalElement.FieldDelimiter.toString)
      .getBytes()
  )

  "validateIp" should {
    "ignore empty IP" in {
      DigitalElement.validateIp(None) should be (Right(None))
      DigitalElement.validateIp(Some("        ")) should be (Right(None))
      DigitalElement.validateIp(Some("  192.168.1.1  ")) should be (
        Right(
          Some(
            ValidatedIpAddress("192.168.1.1", BigInt("3232235777")
          )
        )
      ))
    }
  }

  "ip2decimal" should {
    "correctly Convert ip4 addresses" in {
      DigitalElement.ipToDecimal("0.0.0.0") should be(Right(BigInt("0")))
      DigitalElement.ipToDecimal("192.168.1.1") should be(Right(BigInt("3232235777")))
      DigitalElement.ipToDecimal("255.255.255.255") should be(Right(BigInt("4294967295")))
      DigitalElement.ipToDecimal("::ffff:255.255.255.255") should be(Right(BigInt("4294967295")))
    }

    "correctly Convert ip6 addresses" in {
      DigitalElement.ipToDecimal("2001:0:0:0:0:0:0:0") should be(Right(BigInt("2306124484190404608")))
      DigitalElement.ipToDecimal("2001::0:0::0:0:0") should be(Right(BigInt("2306124484190404608")))
      DigitalElement.ipToDecimal("2404:440c:1463:0:0:0:0:0") should be(Right(BigInt("2595274103944577024")))
    }

    "fail" in {
      def test(ip: String) = {
        DigitalElement.ipToDecimal(ip).left.value shouldBe
          LocationError(LocationErrorCode.IpInvalid, Seq(s"Unable to parse ip address $ip"))
      }
      test("00.0.0")
      test("0.x.0.0")
      test("2001:0:0:0:0:0:0")
      test("2001:0:0:g:0:0:0:0")
    }
  }

  "lookup" should {
    "find the correct range" in {
      val fixture: DigitalElementIndex = TreeRangeMap.create()
      fixture.put(GuavaRange.closed(0, 10), indexRecordFactory(country = Countries.Usa))
      fixture.put(GuavaRange.closed(11, 20), indexRecordFactory(country = Countries.Fra))
      fixture.put(GuavaRange.closed(21, 30), indexRecordFactory(country = Countries.Can))

      println(fixture.lookup(15).value.toAddress)
      fixture.lookup(15).value.toAddress.country.value shouldBe Countries.Fra.iso31663
    }

    "find the correct range when ranges overlap" in {
      // even though this should never happen with real DE data,
      // test is provided for predictability
      val fixture: DigitalElementIndex = TreeRangeMap.create()
      fixture.put(GuavaRange.closed(0, 15), indexRecordFactory(country = Countries.Usa))
      fixture.put(GuavaRange.closed(11, 20), indexRecordFactory(country = Countries.Fra))
      fixture.put(GuavaRange.closed(21, 30), indexRecordFactory(country = Countries.Can))

      fixture.lookup(15).value.toAddress.country.value shouldBe Countries.Fra.iso31663
    }

    "Return None when there is no matching range" in {
      // also should never happen with real DE data as ip ranges are contiguous
      val fixture: DigitalElementIndex = TreeRangeMap.create()
      fixture.put(GuavaRange.closed(0, 15), indexRecordFactory(country = Countries.Usa))
      fixture.put(GuavaRange.closed(20, 30), indexRecordFactory(country = Countries.Fra))
      fixture.put(GuavaRange.closed(31, 40), indexRecordFactory(country = Countries.Can))

      fixture.lookup(17) shouldBe None
    }

    "find the correct range (boundary cases)" in {
      val fixture: DigitalElementIndex = TreeRangeMap.create()
      fixture.put(GuavaRange.closed(0, 10), indexRecordFactory(country = Countries.Usa))
      fixture.put(GuavaRange.closed(11, 20), indexRecordFactory(country = Countries.Fra))
      fixture.put(GuavaRange.closed(21, 30), indexRecordFactory(country = Countries.Can))

      fixture.lookup(11).value.toAddress.country.value shouldBe Countries.Fra.iso31663
      fixture.lookup(20).value.toAddress.country.value shouldBe Countries.Fra.iso31663
    }

    "Return None if the input is completely out of range" in {
      val fixture: DigitalElementIndex = TreeRangeMap.create()
      fixture.put(GuavaRange.closed(5, 10), indexRecordFactory(country = Countries.Usa))
      fixture.put(GuavaRange.closed(11, 20), indexRecordFactory(country = Countries.Fra))
      fixture.put(GuavaRange.closed(21, 30), indexRecordFactory(country = Countries.Can))

      fixture.lookup(4) shouldBe None
      fixture.lookup(15).value.toAddress.country.value shouldBe Countries.Fra.iso31663
      fixture.lookup(31) shouldBe None
    }
  }

  "toAddress" should {
    "properly format an address" in {
      val fixture: DigitalElementIndex = TreeRangeMap.create()
      val record =
        DigitalElementIndexRecord(bytes = "1153680280;1153680287;usa;nj;hoboken;40.7478;-74.0339;###;".getBytes())
      fixture.put(GuavaRange.closed(1153680280, 1153680287), record)

      val expected = Address(
        city = Some("hoboken"),
        province = Some("New Jersey"),
        postal = None,
        country = Some("USA"),
        latitude = Some("40.7478"),
        longitude = Some("-74.0339")
      )

      fixture.lookup(1153680280).value.toAddress should equal(expected)
    }
  }

}
