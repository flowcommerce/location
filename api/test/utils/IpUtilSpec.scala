package utils

import controllers.Helpers
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class IpUtilSpec extends AnyWordSpecLike with GuiceOneServerPerSuite {
  val helper: Helpers = app.injector.instanceOf[Helpers]

  "ipToDecimal" should {
    "convert ipv4 address to decimal format correctly" in {
      println(Await.result(helper.getLocations(ip = Some("23.16.0.0")), 30.seconds))
      println(IpUtil.ipToDecimal("74.199.148.16"))
      IpUtil.ipToDecimal("23.16.0.0") mustBe Right(386924544)
    }

    "convert ipv6 address to decimal format correctly" ignore {
      // println(Await.result(helper.getLocations(ip = Some("2001:470:1f0b:079c:0:0:0:0")), 30.seconds))
      println(IpUtil.ipToDecimal("2a02:c7c:5a31:8f00:942d:e401:b826:1aa3"))
      println(IpUtil.ipToDecimal("2001:1970:5457:8900::1f9a"))
      println(Await.result(helper.getLocations(ip = Some("2001:470:1f0b:079c:0:0:0:0")), 30.seconds))
      IpUtil.ipToDecimal("2001:0470:1f0b:79c:0:0:0:0") mustBe Right(BigInt("42540578174775828387572776328824356864"))
    }

    "convert ipv6 v2 address to decimal format correctly" in {
      // println(Await.result(helper.getLocations(ip = Some("2001:470:1f0b:79c:0:0:0:0")), 30.seconds))
      println(IpUtil.ipv6ToDecimal("2a02:c7c:5a31:8f00:942d:e401:b826:1aa3"))
      println(IpUtil.ipv6ToDecimal("2001:1970:5457:8900:0:0:0:1f9a"))
      println(IpUtil.expandIPv6Address("2001:1970:5457:8900::0:0:1f9a"))
      println(Await.result(helper.getLocations(ip = Some("2001:470:1f0b:079c:0:0:0:0")), 30.seconds))
      IpUtil.ipv6ToDecimal("2001:470:1f0b:79c:0:0:0:0") mustBe Right(BigInt("42540578174775828387572776328824356864"))
    }
  }

  "expandIPv6Address" should {
    "expand ipv6 address to full notation" when {
      "leading zero suppression" in {
        IpUtil.expandIPv6Address("2001:1970:5457:8900::1f9a") mustBe ("2001:1970:5457:8900:0000:0000:0000:1f9a")
      }
      "zero compression" in {
        IpUtil.expandIPv6Address("2001:1970:5457:8900::0:0:1f9a") mustBe ("2001:1970:5457:8900:0000:0000:0000:1f9a")
      }
      "loopback address" in {
        IpUtil.expandIPv6Address("::1f9a") mustBe ("0000:0000:0000:0000:0000:0000:0000:1f9a")
      }
      /*"unspecified address" in {
        IpUtil.expandIPv6Address("::") mustBe ("0000:0000:0000:0000:0000:0000:0000:0000")
      }*/
    }

    "keep full notation address as is" in {
      IpUtil.expandIPv6Address(
        "2001:1970:50dc:7600:147:cceb:2d23:bf5a",
      ) mustBe ("2001:1970:50dc:7600:0147:cceb:2d23:bf5a")
    }
  }
}
