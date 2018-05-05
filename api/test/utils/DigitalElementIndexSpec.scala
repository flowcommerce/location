package utils

import java.io._
import java.nio.file.{Files, Paths}

import io.flow.reference.data.Timezones
import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}

class DigitalElementIndexSpec extends WordSpec with Matchers {

  "buildIndex" should {

    val path = Paths.get("./test/resources/digitalelement_sample.csv")

    val is = Files.newInputStream(path)

    val index = DigitalElement.buildIndex(new BufferedInputStream(is), ';', '\n')

    "index every record" in {
      index.length shouldBe 3254
    }

    "properly parse records" in {
      index(1000).rangeStart should equal(1111906570)
      index(1000).rangeEnd should equal(1111906659)
      index(1000).fieldDelimiter should equal(';')
      index(1000).bytes should equal("1111906570;1111906659;usa;nj;hoboken;40.7478;-74.0339;###;\n".getBytes())
    }

    "timezone" in {
      val data = Map(
        "37.187.99.146" -> Timezones.EuropeParis,
        "23.16.0.0" -> Timezones.AmericaEdmonton,
        "185.236.200.99" -> Timezones.AmericaLosAngeles,
        "173.63.71.187" -> Timezones.AmericaNewYork
      )

      val invalid = data.keys.toSeq.flatMap { ip =>
        val ipInt = DigitalElement.ipToDecimal(ip) match {
          case Failure(_) => sys.error(s"Missing ip '$ip'")
          case Success(v) => v
        }

        val element = index.lookup(ipInt).getOrElse {
          sys.error(s"ip '$ip' missing with intvalue '$ipInt'")
        }

        val tz = element.timezone.getOrElse {
          sys.error(s"ip '$ip' missing timezone")
        }

        if (tz == data(ip)) {
          None
        } else {
          Some(s"IP '$ip' expected timezone[${data(ip).name}] but got '${tz.name}'")
        }
      }
      invalid should equal(Nil)
    }
  }

}
