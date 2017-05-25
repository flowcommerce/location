package utils

import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}

class DigitalElementSpec extends WordSpec with Matchers {

  // convenience to build EdgeRecord fixtures with defaults
  def edgeRecordFactory(
    rangeStart: Long = Long.MinValue,
    rangeEnd  : Long = Long.MaxValue,
    country   : String = "",
    region    : String = "",
    city      : String = "",
    latitude: Double = 0.0,
    longitude : Double = 0.0,
    postalCode: String = "",
    countryCode: Int = 0,
    regionCode: Int = 0,
    cityCode: Int = 0,
    continentCode: Int = 0,
    twoLetterCountry: String = "",
    gmtOffset: String = "",
    inDst: Boolean = false
  ) = EdgeRecord(rangeStart, rangeEnd, country, region, city, latitude, longitude, postalCode,
    countryCode, regionCode, cityCode, continentCode, twoLetterCountry, gmtOffset, inDst)

  "ip2decimal" should {
    "correctly Convert ip addresses" in {
      DigitalElement.ipToDecimal("0.0.0.0") should be(Success(0L))
      DigitalElement.ipToDecimal("192.168.1.1") should be(Success(3232235777L))
      DigitalElement.ipToDecimal("255.255.255.255") should be(Success(4294967295L))
    }

    "fail" in {
      DigitalElement.ipToDecimal("00.0.0") shouldBe a[Failure[_]]
      DigitalElement.ipToDecimal("0.x.0.0") shouldBe a[Failure[_]]
    }
  }

  "lookup" should {
    "find the correct range" in {
      val fixture = IndexedSeq(
        edgeRecordFactory(rangeStart = 0, rangeEnd = 10),
        edgeRecordFactory(rangeStart = 11, rangeEnd = 20),
        edgeRecordFactory(rangeStart = 21, rangeEnd = 30)
      )
      DigitalElement.lookup(15, fixture) should equal(Some(fixture(1)))
    }

    "find the correct range when ranges overlap" in {
      // even though this should never happen with real DE data,
      // test is provided for predictability
      val fixture = IndexedSeq(
        edgeRecordFactory(rangeStart = 0, rangeEnd = 15),
        edgeRecordFactory(rangeStart = 11, rangeEnd = 20),
        edgeRecordFactory(rangeStart = 21, rangeEnd = 30)
      )
      DigitalElement.lookup(13, fixture) should equal(Some(fixture(1)))
    }

    "Return None when there is no matching range" in {
      // also should never happen with real DE data as ip ranges are contiguous
      val fixture = IndexedSeq(
        edgeRecordFactory(rangeStart = 0, rangeEnd = 15),
        edgeRecordFactory(rangeStart = 20, rangeEnd = 30),
        edgeRecordFactory(rangeStart = 31, rangeEnd = 40)
      )
      DigitalElement.lookup(17, fixture) shouldBe None
    }

    "find the correct range (boundary cases)" in {
      val fixture = IndexedSeq(
        edgeRecordFactory(rangeStart = 0, rangeEnd = 10),
        edgeRecordFactory(rangeStart = 11, rangeEnd = 20),
        edgeRecordFactory(rangeStart = 21, rangeEnd = 30)
      )
      DigitalElement.lookup(11, fixture) should equal(Some(fixture(1)))
      DigitalElement.lookup(20, fixture) should equal(Some(fixture(1)))
    }

    "Return None if the input is completely out of range" in {
      val fixture = IndexedSeq(
        edgeRecordFactory(rangeStart = 5, rangeEnd = 10),
        edgeRecordFactory(rangeStart = 11, rangeEnd = 20),
        edgeRecordFactory(rangeStart = 21, rangeEnd = 30)
      )
      DigitalElement.lookup(4, fixture) shouldBe None
      DigitalElement.lookup(31, fixture) shouldBe None
    }
  }

}
