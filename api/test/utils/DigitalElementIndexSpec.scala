package utils

import java.io._
import java.nio.file.{Files, Paths}
import org.scalatest.{Matchers, WordSpec}

class DigitalElementIndexSpec extends WordSpec with Matchers {

  "buildIndex" should {

    val path = Paths.get("./test/resources/digitalelement_sample.csv")

    val is = Files.newInputStream(path)

    val index = DigitalElement.buildIndex(new BufferedInputStream(is), '\n')

    "index every record" in {
      index.asMapOfRanges().size() shouldBe 3254
    }

    "properly parse records" in {
      index.get(1111906570).bytes should equal("1111906570;1111906659;usa;nj;hoboken;40.7478;-74.0339;###;\n".getBytes())
    }

    "timezone" in {
      val invalid = DigitalElementSampleData.IpTimezones.keys.toSeq.flatMap { ip =>
        val ipInt = DigitalElement.ipToDecimal(ip).right.getOrElse {
          sys.error(s"Missing ip '$ip'")
        }

        val element = index.lookup(ipInt).getOrElse {
          sys.error(s"ip '$ip' missing with intvalue '$ipInt'")
        }

        val tz = element.timezone.getOrElse {
          sys.error(s"ip '$ip' missing timezone")
        }

        if (tz == DigitalElementSampleData.IpTimezones(ip)) {
          None
        } else {
          Some(s"IP '$ip' expected timezone[${DigitalElementSampleData.IpTimezones(ip).name}] but got '${tz.name}'")
        }
      }
      invalid should equal(Nil)
    }
  }

}
