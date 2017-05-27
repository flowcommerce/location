package utils

import java.io._
import java.nio.channels.FileChannel
import java.nio.file.{Files, Paths}
import java.io.BufferedReader

import org.scalatest.{Matchers, WordSpec}


class DigitalElementIndexSpec extends WordSpec with Matchers {

  "buildIndex" should {

    val path = Paths.get("./test/resources/digitalelement_sample.csv")

    val is = Files.newInputStream(path)

    val index = DigitalElement.buildIndex(new BufferedInputStream(is), ';', '\n')

    "index every record" in {
      index.length shouldBe 3368
    }

    "properly parse records" in {
      index(1000).rangeStart should equal(1111904306)
      index(1000).rangeEnd should equal(1111904315)
      index(1000).fieldDelimiter should equal(';')
      index(1000).bytes should equal("1111904306;1111904315;usa;nj;hoboken;40.7478;-74.0339;###;840;31;3293;6;us;-400;y;\n".getBytes())
    }

  }

}
