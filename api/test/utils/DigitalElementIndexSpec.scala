package utils

import java.io._
import java.nio.file.{Files, Paths}

import io.flow.reference.data.{Countries, Provinces}
import org.scalatest.{Matchers, OptionValues, WordSpec}

class DigitalElementIndexSpec extends WordSpec with Matchers with OptionValues {

  "buildIndex" should {

    val path = Paths.get("./test/resources/digitalelement_sample.csv")

    val is = Files.newInputStream(path)

    val index = DigitalElement.buildIndex(new BufferedInputStream(is), '\n')

    "index every record" in {
      index.asMapOfRanges().size() shouldBe 3254
    }

    "properly parse records" in {
      // "1111906570;1111906659;usa;nj;hoboken;40.7478;-74.0339;###;"
      val address = index.get(1111906570).address
      address.country.value shouldBe Countries.Usa.iso31663
      address.province.value shouldBe Provinces.UsaNj.iso31662
      address.city.value shouldBe "hoboken"
      address.latitude.value shouldBe "40.7478"
      address.longitude.value shouldBe "-74.0339"
      address.postal shouldBe None

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
