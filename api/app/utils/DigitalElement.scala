package utils
import java.io.{BufferedReader, InputStream}
import java.nio.MappedByteBuffer
import java.nio.charset.StandardCharsets

import scala.collection.mutable
import scala.util.Try

case class DigitalElementIndexRecord(
  rangeStart: Long,
  fieldDelimiter: Char,
  bytes: Array[Byte]) extends Ordered[DigitalElementIndexRecord] {

  override def compare(that: DigitalElementIndexRecord): Int = (this.rangeStart - that.rangeStart).toInt
}

/**
  * Represents a record from DigitalElement's Edge database
  * https://portal.digitalelement.com/portal/running/descriptions.html
  *
  * @param rangeStart
  * @param rangeEnd
  * @param country
  * @param region
  * @param city
  * @param latitude
  * @param longitude
  * @param postalCode
  * @param countryCode
  * @param regionCode
  * @param cityCode
  * @param continentCode
  * @param twoLetterCountry
  * @param gmtOffset
  * @param inDst
  */
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
  twoLetterCountry: String,
  gmtOffset: String,
  inDst: Boolean
) extends Ordered[EdgeRecord] {
  override def compare(that: EdgeRecord): Int = (this.rangeStart - that.rangeStart).toInt
}

object EdgeRecord {
  def fromByteArray(b: Array[Byte], fieldDelimiter: Char): EdgeRecord = {
    val fields = new String(b).split(fieldDelimiter)
    EdgeRecord(
      rangeStart = fields(0).toLong,
      rangeEnd = fields(1).toLong,
      country = fields(2),
      region = fields(3),
      city = fields(4),
      latitude = fields(5).toDouble,
      longitude = fields(6).toDouble,
      postalCode = fields(7),
      countryCode = fields(8).toInt,
      regionCode = fields(9).toInt,
      cityCode = fields(10).toInt,
      continentCode = fields(11).toInt,
      twoLetterCountry = fields(12),
      gmtOffset = fields(13),
      inDst = fields(14) match {
        case "y" => true
        case _ => false
      }
    )
  }
}

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

  def lookup(ip: Long, index: IndexedSeq[EdgeRecord]) =
      Collections.searchWithBoundary(index, ip)((a,b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)

  def buildIndex(buf: InputStream, fieldDelimiter: Char, recordDelimiter: Char): IndexedSeq[DigitalElementIndexRecord] = {
    val rd = recordDelimiter.toByte
    val fd = fieldDelimiter.toByte
    val indexBuilder = IndexedSeq.newBuilder[DigitalElementIndexRecord]
    val recordBuilder = new mutable.ArrayBuilder.ofByte
    val rangeStartBuilder = new mutable.ArrayBuilder.ofByte
    var counter = 0
    var rangeStartFound = false
    var cur: Int = 0
    cur = buf.read()
    while(cur != -1) {
      cur match {
        case `rd` => {
          val rangeStart = new String(rangeStartBuilder.result()).toLong
          val bytes = recordBuilder.result()
          val rec = DigitalElementIndexRecord(rangeStart, fieldDelimiter, bytes)
          indexBuilder += rec
          recordBuilder.clear()
          rangeStartBuilder.clear()
          rangeStartFound = false
          counter = counter + 1;
          if (counter % 100000 == 0) {
            System.out.println(s"Indexed ${counter} records")
          }
        }
        case _ => {
          val b = cur.toByte
          if(b == fd && !rangeStartFound) {
            rangeStartFound = true
          } else if (!rangeStartFound) {
            rangeStartBuilder += b
          }
          recordBuilder += b
        }
      }
      cur = buf.read()
    }
    indexBuilder.result()
  }


}
