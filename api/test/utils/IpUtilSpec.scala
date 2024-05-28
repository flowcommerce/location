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
      IpUtil.ipToDecimal("23.16.0.0") mustBe Right(386924544)
    }

    "convert ipv6 address to decimal format correctly" in {
      println(Await.result(helper.getLocations(ip = Some("2001:470:1f0b:079c:0:0:0:0")), 30.seconds))
      IpUtil.ipToDecimal("2001:0470:1f0b:79c:0:0:0:0") mustBe Right(BigInt("42540578174775828387572776328824356864"))
    }

    "convert ipv6 v2 address to decimal format correctly" in {
      println(Await.result(helper.getLocations(ip = Some("2001:470:1f0b:79c:0:0:0:0")), 30.seconds))
      IpUtil.ipv6ToDecimal("2001:470:1f0b:79c:0:0:0:0") mustBe Right(BigInt("42540578174775828387572776328824356864"))
    }
  }

}
