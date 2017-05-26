package utils
import java.io.InputStream

import io.flow.common.v0.models.Address

import scala.collection.mutable
import scala.util.Try

case class DigitalElementIndexRecord(
  rangeStart: Long,
  rangeEnd: Long,
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

  def fromIndexRecord(r: DigitalElementIndexRecord): EdgeRecord =
    fromByteArray(r.bytes, r.fieldDelimiter)

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

  def toAddress(ir: DigitalElementIndexRecord): Address = {
    val fields = new String(ir.bytes).split(ir.fieldDelimiter)
    Address(
      city = Some(fields(4)),
      province = Some(fields(3)),
      postal = Some(fields(7)),
      country = io.flow.reference.Countries.find(fields(2)).map(_.iso31663),
      latitude = Some(fields(5)),
      longitude = Some(fields(6))
      )
  }

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

  def lookup(ip: Long, index: IndexedSeq[DigitalElementIndexRecord]): Option[DigitalElementIndexRecord] =
      Collections.searchWithBoundary(index, ip)((a,b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)

  def buildIndex(buf: InputStream, fieldDelimiter: Char, recordDelimiter: Char): IndexedSeq[DigitalElementIndexRecord] = {
    System.out.println(s"using ioStream ${buf} with ${buf.available()}")
    val rd = recordDelimiter.toByte
    val fd = fieldDelimiter.toByte
    val indexBuilder = IndexedSeq.newBuilder[DigitalElementIndexRecord]
    val recordBuilder = new mutable.ArrayBuilder.ofByte
    var charCounter = -1
    var firstFieldLimit = -1
    var secondFieldLimit = -1
    var cur: Int = 0
    cur = buf.read()
    while(cur != -1) {
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
          val rangeStart = new String(bytes.slice(0,firstFieldLimit)).toLong
          val rangeEnd = new String(bytes.slice(firstFieldLimit+1,secondFieldLimit)).toLong
          val rec = DigitalElementIndexRecord(rangeStart, rangeEnd, fieldDelimiter, bytes)
          indexBuilder += rec
          recordBuilder.clear()
          charCounter = -1
          firstFieldLimit = -1
          secondFieldLimit = -1
        }
        case _ => ()
      }
      cur = buf.read()
    }
    indexBuilder.result()
  }


}
