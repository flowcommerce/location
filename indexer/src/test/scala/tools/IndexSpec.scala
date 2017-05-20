package tools

import java.io.{BufferedInputStream, File, FileInputStream, RandomAccessFile}
import java.nio.channels.FileChannel

import org.scalatest.{Matchers, WordSpec}
import utils.DigitalElement


class IndexSpec extends WordSpec with Matchers {

  "makeIndex" should {

    val resource = getClass().getResource("/digital_element_sample.txt")

    val path = resource.getFile()

    val file = new File(path)

    //val file = new File("/Users/eric/netacuity/text_file_2/output.csv")

    val channel = new RandomAccessFile(file, "r").getChannel()

    val mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

    val start = System.currentTimeMillis()
    System.out.println("Making index")
    val index = Index.makeIndex(mapped, ';', '\n')
    val end = System.currentTimeMillis()
    System.out.println(s"Indexed ${index.size} records in ${(end - start) / 1000} secs")

    System.out.println(s"First record:\n\t${index(0)}")
    val record1 = new Array[Byte](index(0).length)
    mapped.position(index(0).fileOffset)
    mapped.get(record1, 0, index(0).length)
    System.out.println(s"parsed:\n\t${new String(record1)}")

    System.out.println(s"500th record:\n\t${index(499)}")
    val record500 = new Array[Byte](index(499).length)
    mapped.position(index(499).fileOffset)
    mapped.get(record500, 0, index(499).length)
    System.out.println(s"parsed:\n\t${new String(record500)}")

    val last = index.size - 1
    System.out.println(s"Last record:\n\t${index(last)}")
    val lastRecord = new Array[Byte](index(last).length)
    mapped.position(index(last).fileOffset)
    mapped.get(lastRecord, 0, index(last).length)
    System.out.println(s"parsed:\n\t${new String(lastRecord)}")


  }

}
