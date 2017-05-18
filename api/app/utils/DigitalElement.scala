package utils
import java.nio.MappedByteBuffer
import java.nio.charset.StandardCharsets

import scala.collection.mutable

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
