package utils

import io.flow.common.v0.models.Address
import io.flow.location.v0.models.{LocationError, LocationErrorCode}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utils.DigitalElement.ValidatedIpAddress
import scala.collection.mutable.ArrayBuffer
class DigitalElementSpec extends AnyWordSpec with Matchers {

  // convenience to build DigitalElementIndexRecord fixtures with defaults
  def indexRecordFactory(
    rangeStart: BigInt = Long.MinValue,
    rangeEnd: BigInt = Long.MaxValue,
    country: String = "",
    region: String = "",
    city: String = "",
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    postalCode: String = "",
    fieldDelimiter: Char = ';',
  ): DigitalElementIndexRecord = {
    val bytes: Array[Byte] =
      ArrayBuffer[Serializable](rangeStart, rangeEnd, country, region, city, latitude, longitude, postalCode)
        .map(_.toString)
        .mkString(fieldDelimiter.toString)
        .getBytes()
    DigitalElementIndexRecord(rangeStart, rangeEnd, fieldDelimiter, bytes)
  }

  "validateIp" should {
    "ignore empty IP" in {
      DigitalElement.validateIp(None) should be(Right(None))
      DigitalElement.validateIp(Some("        ")) should be(Right(None))
      DigitalElement.validateIp(Some("  192.168.1.1  ")) should be(
        Right(
          Some(
            ValidatedIpAddress("192.168.1.1", BigInt("3232235777")),
          ),
        ),
      )
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
      DigitalElement.ipToDecimal("2001:0:0:0:0:0:0") should be(Right(BigInt("2306124484190404608")))

      DigitalElement.ipToDecimal("2600:3:8:f::b4") should be(Right(BigInt("2738188586326687759")))
      DigitalElement.ipToDecimal("2a03:28:ff:f::face:b00c") should be(Right(BigInt("3027263546338508815")))
      DigitalElement.ipToDecimal("2a03:28:ff:f") should be(Right(BigInt("3027263546338508815")))
    }

    "fail" in {
      def test(ip: String) = {
        DigitalElement.ipToDecimal(ip) shouldBe (
          Left(LocationError(LocationErrorCode.IpInvalid, Seq(s"Unable to parse ip address $ip")))
        )
      }
      test("00.0.0")
      test("0.x.0.0")
      test("2001:0:0:g:0:0:0:0")
    }
  }

  "lookup" should {
    "find the correct range" in {
      val fixture = IndexedSeq(
        indexRecordFactory(rangeStart = 0, rangeEnd = 10),
        indexRecordFactory(rangeStart = 11, rangeEnd = 20),
        indexRecordFactory(rangeStart = 21, rangeEnd = 30),
      )
      fixture.lookup(15) should equal(Some(fixture(1)))
    }

    "find the correct range when ranges overlap" in {
      // even though this should never happen with real DE data,
      // test is provided for predictability
      val fixture = IndexedSeq(
        indexRecordFactory(rangeStart = 0, rangeEnd = 15),
        indexRecordFactory(rangeStart = 11, rangeEnd = 20),
        indexRecordFactory(rangeStart = 21, rangeEnd = 30),
      )
      fixture.lookup(13) should equal(Some(fixture(1)))
    }

    "Return None when there is no matching range" in {
      // also should never happen with real DE data as ip ranges are contiguous
      val fixture = IndexedSeq(
        indexRecordFactory(rangeStart = 0, rangeEnd = 15),
        indexRecordFactory(rangeStart = 20, rangeEnd = 30),
        indexRecordFactory(rangeStart = 31, rangeEnd = 40),
      )
      fixture.lookup(17) shouldBe None
    }

    "find the correct range (boundary cases)" in {
      val fixture = IndexedSeq(
        indexRecordFactory(rangeStart = 0, rangeEnd = 10),
        indexRecordFactory(rangeStart = 11, rangeEnd = 20),
        indexRecordFactory(rangeStart = 21, rangeEnd = 30),
      )
      fixture.lookup(11) should equal(Some(fixture(1)))
      fixture.lookup(20) should equal(Some(fixture(1)))
    }

    "Return None if the input is completely out of range" in {
      val fixture = IndexedSeq(
        indexRecordFactory(rangeStart = 5, rangeEnd = 10),
        indexRecordFactory(rangeStart = 11, rangeEnd = 20),
        indexRecordFactory(rangeStart = 21, rangeEnd = 30),
      )
      fixture.lookup(4) shouldBe None
      fixture.lookup(31) shouldBe None
    }
  }

  "toAddress" should {
    "properly format an address" in {
      val fixture = DigitalElementIndexRecord(
        rangeStart = 1153680280,
        rangeEnd = 1153680287,
        fieldDelimiter = ';',
        bytes = "1153680280;1153680287;usa;nj;hoboken;40.7478;-74.0339;###;".getBytes(),
      )

      val expected = Address(
        city = Some("hoboken"),
        province = Some("New Jersey"),
        postal = None,
        country = Some("USA"),
        latitude = Some("40.7478"),
        longitude = Some("-74.0339"),
      )

      fixture.toAddress should equal(expected)
    }
  }

}
