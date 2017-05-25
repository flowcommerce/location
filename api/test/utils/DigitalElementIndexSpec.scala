package utils

import java.io._
import java.nio.channels.FileChannel
import java.nio.file.{Files, Paths}
import java.io.BufferedReader

import org.scalatest.{Matchers, WordSpec}


class DigitalElementIndexSpec extends WordSpec with Matchers {

  "buildIndex" should {

    val path = Paths.get("/Users/eric/netacuity/text_file_2/output.csv")

    val is = Files.newInputStream(path)

    val start = System.currentTimeMillis()
    System.out.println("Building index")
    val index = DigitalElement.buildIndex(new BufferedInputStream(is, 4000000), ';', '\n')
    val end = System.currentTimeMillis()
    System.out.println(s"Indexed ${index.size} records in ${(end - start) / 1000} secs")

    System.out.println(s"First record:\n\t${index(0)}")

    System.out.println(s"500th record:\n\t${index(499)}")

    val last = index.size - 1
    System.out.println(s"Last record:\n\t${index(last)}")


  }

}
