package utils

import controllers.Helpers
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class IpUtilSpec extends AnyWordSpecLike with GuiceOneServerPerSuite {
  val helper: Helpers = app.injector.instanceOf[Helpers]

  "ipToDecimal" should {
    "convert ipv4 address to decimal format correctly" in {
      IpUtil.ipToDecimal("23.16.0.0") mustBe Right(386924544)
    }

    "convert ipv6 address to decimal format correctly" in {
      IpUtil.ipToDecimal("2001:0470:1f0b:79c:0:0:0:0") mustBe Right(BigInt("42540578174775828387572776328824356864"))
      IpUtil.ipToDecimal("::ffff:106:0") mustBe Right(BigInt("281470698913792"))
    }
  }

  "expandIPv6Address" should {
    "expand ipv6 address to full notation" when {
      "it is leading zero suppression" in {
        IpUtil.expandIfIPv6Address("2001:1970:5457:8900::1f9a") mustBe "2001:1970:5457:8900:0000:0000:0000:1f9a"
        IpUtil.expandIfIPv6Address("2001:0470:1f0b:079c::") mustBe "2001:0470:1f0b:079c:0000:0000:0000:0000"
      }
      "it is zero compression" in {
        IpUtil.expandIfIPv6Address("2001:1970:5457:8900::0:0:1f9a") mustBe "2001:1970:5457:8900:0000:0000:0000:1f9a"
      }

      "it is loopback" in {
        IpUtil.expandIfIPv6Address("::1f9a") mustBe "0000:0000:0000:0000:0000:0000:0000:1f9a"
        IpUtil.expandIfIPv6Address("::ffff:106:0") mustBe "0000:0000:0000:0000:0000:ffff:0106:0000"
      }

      "it is unspecified" in {
        IpUtil.expandIfIPv6Address("::") mustBe "0000:0000:0000:0000:0000:0000:0000:0000"
      }

      "it is full notation" in {
        IpUtil.expandIfIPv6Address(
          "2001:1970:50dc:7600:147:cceb:2d23:bf5a",
        ) mustBe "2001:1970:50dc:7600:0147:cceb:2d23:bf5a"
      }
    }

    "not expand addresses" when {
      "address is already fully expanded" in {
        IpUtil.expandIfIPv6Address(
          "2001:1970:50dc:7600:0147:cceb:2d23:bf5a",
        ) mustBe "2001:1970:50dc:7600:0147:cceb:2d23:bf5a"
      }

      "it is an ipv4 address" in {
        IpUtil.expandIfIPv6Address("192.168.4.1") mustBe "192.168.4.1"
      }
    }

  }
}
