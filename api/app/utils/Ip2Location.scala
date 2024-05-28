package utils

import io.flow.common.v0.models.Address
import io.flow.reference.Countries

import java.io.InputStream
import javax.inject.Singleton
import scala.collection.mutable

case class Ip2Location(rangeStart: BigInt, rangeEnd: BigInt, fieldDelimiter: Char, bytes: Array[Byte]) {
  def toAddress: Address = {
    val row = new String(bytes).split(",")
    Address(country = Countries.find(row(2)).map(_.iso31663))
  }
}

@Singleton
object Ip2Location {

  def buildIndex(is: InputStream, fieldDelimiter: Char, recordDelimiter: Char): IndexedSeq[Ip2Location] = {
    val rd = recordDelimiter.toByte // \n
    val fd = fieldDelimiter.toByte // ;
    val indexBuilder = IndexedSeq.newBuilder[Ip2Location]
    val recordBuilder = new mutable.ArrayBuilder.ofByte
    var charCounter = -1
    var firstFieldLimit = -1
    var secondFieldLimit = -1
    var cur: Int = 0
    cur = is.read()
    while (cur != -1) {
      val b = cur.toByte
      recordBuilder += b
      charCounter = charCounter + 1
      b match {
        case `fd` if (firstFieldLimit == -1) => {
          firstFieldLimit = charCounter
        }
        case `fd` if (secondFieldLimit == -1) => {
          secondFieldLimit = charCounter
        }
        case `rd` => {
          val bytes = recordBuilder.result()
          val rangeStart = BigInt(new String(bytes.slice(0, firstFieldLimit)))
          val rangeEnd = BigInt(new String(bytes.slice(firstFieldLimit + 1, secondFieldLimit)))
          val rec = Ip2Location(rangeStart, rangeEnd, fieldDelimiter, bytes)
          indexBuilder += rec
          recordBuilder.clear()
          charCounter = -1
          firstFieldLimit = -1
          secondFieldLimit = -1
        }
        case _ => ()
      }
      cur = is.read()
    }
    indexBuilder.result()
  }
}
