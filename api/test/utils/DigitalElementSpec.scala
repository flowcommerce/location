package utils

import java.io.{File, FileInputStream, RandomAccessFile}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import org.scalatest.{Matchers, WordSpec}

class DigitalElementSpec extends WordSpec with Matchers {

  "makeIndex" should {

    val resource = getClass().getResource("/digital_element_sample.txt")

    val path = resource.getFile()

    val file = new File(path)

    val channel = new RandomAccessFile(file, "r").getChannel()

    val mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

    val index = DigitalElement.makeIndex(mapped, ';', '\n')

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

    System.out.println(s"Last record:\n\t${index(999)}")
    val record999 = new Array[Byte](index(999).length)
    mapped.position(index(999).fileOffset)
    mapped.get(record999, 0, index(999).length)
    System.out.println(s"parsed:\n\t${new String(record999)}")


  }

}
