package utils;

import io.flow.reference.data.Countries
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.BufferedInputStream
import java.nio.file.{Files, Paths};

class Ip2LocationSpec extends AnyWordSpec with Matchers {
  "buildIndex" should {
    "correctly index ipv4 addresses" when {
      val path = Paths.get("./test/resources/IP-COUNTRY(in).csv")
      val is = new BufferedInputStream(Files.newInputStream(path))
      val result = Ip2Location.buildIndex(is, fieldDelimiter = ',', recordDelimiter = '\n')

      val AusRangeStart = 17039360
      val AusRangeEnd = 17039615
      val AusRangeStartBoundary = 17039361
      val AusRangeEndBoundary = 17039614
      val AusBetweenRange = 17039415

      val Chn1RangeStart = 16973824
      val Chn1RangeEnd = 17039359
      val Chn1RangeStartBoundary = 16973825
      val Chn1RangeEndBoundary = 17039358
      val Chn1BetweenRange = 17029155

      val Chn2RangeStart = 17039616
      val Chn2RangeEnd = 17072127
      val Chn2RangeStartBoundary = 17039617
      val Chn2RangeEndBoundary = 17072126
      val Chn2BetweenRange = 17040011

      "value range start is provided" in {
        result.lookup(AusRangeStart).get.toAddress().country.get mustBe Countries.Aus.iso31663

        result.lookup(Chn1RangeStart).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2RangeStart).get.toAddress().country.get mustBe Countries.Chn.iso31663
      }
      "value range end is provided" in {
        result.lookup(AusRangeEnd).get.toAddress().country.get mustBe Countries.Aus.iso31663

        result.lookup(Chn1RangeEnd).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2RangeEnd).get.toAddress().country.get mustBe Countries.Chn.iso31663

      }
      "value on the boundary of range start and range end is provided" in {
        result.lookup(AusRangeStartBoundary).get.toAddress().country.get mustBe Countries.Aus.iso31663
        result.lookup(AusRangeEndBoundary).get.toAddress().country.get mustBe Countries.Aus.iso31663

        result.lookup(Chn1RangeStartBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663
        result.lookup(Chn1RangeEndBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2RangeStartBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663
        result.lookup(Chn2RangeEndBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663
      }
      "value in between range start and range end is provided" in {
        result.lookup(AusBetweenRange).get.toAddress().country.get mustBe Countries.Aus.iso31663

        result.lookup(Chn1BetweenRange).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2BetweenRange).get.toAddress().country.get mustBe Countries.Chn.iso31663
      }

    }

    "correctly index ipv6 addresses" when {
      val path = Paths.get("./test/resources/IPV6-COUNTRY.csv")
      val is = new BufferedInputStream(Files.newInputStream(path))
      val result = Ip2Location.buildIndex(is, fieldDelimiter = ',', recordDelimiter = '\n')

      val UsRangeStart = 281470698586368L
      val UsRangeEnd = 281470698586623L
      val UsRangeStartBoundary = 281470698586369L
      val UsRangeEndBoundary = 281470698586622L
      val UsBetweenRange = 281470698586586L

      val Chn1RangeStart = 281470698586112L
      val Chn1RangeEnd = 281470698586367L
      val Chn1RangeStartBoundary = 281470698586113L
      val Chn1RangeEndBoundary = 281470698586366L
      val Chn1BetweenRange = 281470698586232L

      val Chn2RangeStart = 281470698586624L
      val Chn2RangeEnd = 281470698602495L
      val Chn2RangeStartBoundary = 281470698586625L
      val Chn2RangeEndBoundary = 281470698602494L
      val Chn2BetweenRange = 281470698596752L

      "value range start is provided" in {
        result.lookup(UsRangeStart).get.toAddress().country.get mustBe Countries.Usa.iso31663

        result.lookup(Chn1RangeStart).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2RangeStart).get.toAddress().country.get mustBe Countries.Chn.iso31663
      }
      "value range end is provided" in {
        result.lookup(UsRangeEnd).get.toAddress().country.get mustBe Countries.Usa.iso31663

        result.lookup(Chn1RangeEnd).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2RangeEnd).get.toAddress().country.get mustBe Countries.Chn.iso31663

      }
      "value on the boundary of range start and range end is provided" in {
        result.lookup(UsRangeStartBoundary).get.toAddress().country.get mustBe Countries.Usa.iso31663
        result.lookup(UsRangeEndBoundary).get.toAddress().country.get mustBe Countries.Usa.iso31663

        result.lookup(Chn1RangeStartBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663
        result.lookup(Chn1RangeEndBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2RangeStartBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663
        result.lookup(Chn2RangeEndBoundary).get.toAddress().country.get mustBe Countries.Chn.iso31663
      }
      "value in between range start and range end is provided" in {
        result.lookup(UsBetweenRange).get.toAddress().country.get mustBe Countries.Usa.iso31663

        result.lookup(Chn1BetweenRange).get.toAddress().country.get mustBe Countries.Chn.iso31663

        result.lookup(Chn2BetweenRange).get.toAddress().country.get mustBe Countries.Chn.iso31663
      }
    }
  }
}
