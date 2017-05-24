package utils

import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}

class DigitalElementSpec extends WordSpec with Matchers {

  "ip2decimal" should {
    "correctly Convert ip addresses" in {
      DigitalElement.ipToDecimal("0.0.0.0") should be(Success(0L))
      DigitalElement.ipToDecimal("192.168.1.1") should be(Success(3232235777L))
      DigitalElement.ipToDecimal("255.255.255.255") should be(Success(4294967295L))
    }

    "fail" in {
      DigitalElement.ipToDecimal("00.0.0") shouldBe a [Failure[_]]
      DigitalElement.ipToDecimal("0.x.0.0") shouldBe a [Failure[_]]
    }
  }

}
