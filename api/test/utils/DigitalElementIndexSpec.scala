package utils

import java.io._
import java.nio.file.{Files, Paths}

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
        "37.187.99.146" -> "France",
        "23.16.0.0" -> "Canada",
        "185.236.200.99" -> "LA",
        "173.63.71.187" -> "NJ"
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

        if (tz.name == data(ip)) {
          None
        } else {
          Some(s"IP '$ip' expected timezone[${data(ip)}] but got '$tz'")
        }
      }
      invalid should equal(Nil)
    }
  }

}
