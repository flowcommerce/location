package utils
import java.nio.MappedByteBuffer
import java.nio.charset.StandardCharsets

import scala.collection.mutable
import scala.util.Try

case class DigitalElementOffset(
  ipRangeStart: Long,
  fileOffset: Int,
  length: Int) extends Ordered[DigitalElementOffset] {

  override def compare(that: DigitalElementOffset): Int = (this.ipRangeStart - that.ipRangeStart).toInt
}

case class EdgeRecord(
  rangeStart: Long,
  rangeEnd: Long,
  country: String,
  region: String,
  city: String,
  latitude: Double,
  longitude: Double,
  postalCode: String,
  countryCode: Int,
  regionCode: Int,
  cityCode: Int,
  continentCode: Int,
  twoLetterCountry: Int,
  gmtOffset: String,
  inDst: Boolean
)


object DigitalElement {

  def ipToDecimal(ip:String): Try[Long] = Try {
    ip.split("\\.").map(Integer.parseInt) match {
      case(Array(a, b, c, d)) => {
        a * scala.math.pow(256, 3).toLong +
          b * scala.math.pow(256, 2).toLong +
          c * scala.math.pow(256, 1).toLong +
          d * scala.math.pow(256, 0).toLong
      }
      case _ => throw new IllegalArgumentException("Unable to parse ip address")
    }

  }

  def lookup(ipString: String, index: Array[EdgeRecord]): Try[Option[EdgeRecord]] = {
    ipToDecimal(ipString) map { ip =>
      Collections.searchWithBoundary(index, ip)((a,b) => a.rangeStart <= b)
    }
  }

  def makeIndex(buf: MappedByteBuffer, fieldDelimiter: Char, recordDelimiter: Char): Array[DigitalElementOffset] = {
    val builder: mutable.ArrayBuilder[DigitalElementOffset] = mutable.ArrayBuilder.make()
    var i = 0
    var curRange = new mutable.StringBuilder()
    var curOffset = 0
    var withinRange = true
    while (buf.hasRemaining()) {
      val c = buf.get()
      c match {
        case `fieldDelimiter` => {
          if (withinRange) {
            withinRange = false
          }
        }
        case `recordDelimiter` => {
          builder += DigitalElementOffset(curRange.toLong, curOffset, buf.position() - curOffset)
          curOffset = buf.position()
          withinRange = true
          curRange.clear()
        }
        case _ => {
          if (withinRange) {
            curRange += c.toChar
          }
        }
      }
      i = i + 1
    }
    builder.result()
  }


}
