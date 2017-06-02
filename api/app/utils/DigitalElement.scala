package utils
import java.io.InputStream

import io.flow.common.v0.models.Address
import io.flow.reference.{Countries, Provinces}

import scala.collection.mutable
import scala.util.Try

/**
  * Searchable format of a DigitalElement ip range record.
  * The expected format of the record byteststring is:
  * ip_range_start;ip_range_end;3_letter_country_code;region;city;latitude;longitude;postal_code;
  *
  * for example:
  * 4264702208;4264702463;usa;wa;seattle;47.6834;-122.291;###;
  *
  * @param rangeStart decimal value of the ip range min
  * @param rangeEnd decimal value of the ip range end
  * @param fieldDelimiter character used to delimit each field
  * @param bytes raw bytestring of the entire record
  */
case class DigitalElementIndexRecord(
  rangeStart: Long,
  rangeEnd: Long,
  fieldDelimiter: Char,
  bytes: Array[Byte]) extends Ordered[DigitalElementIndexRecord] {

  override def compare(that: DigitalElementIndexRecord): Int = (this.rangeStart - that.rangeStart).toInt
}

object DigitalElement {

  /**
    * Constructs an Address from an index record using the format specified
    * for the DigitalElementIndexRecord bytes value
    */
  def toAddress(ir: DigitalElementIndexRecord): Address = {
    val fields = new String(ir.bytes).split(ir.fieldDelimiter)
    val country = Countries.mustFind(fields(2))
    val province = Provinces.find(s"${country.iso31663}-${fields(3)}")
    Address(
      city = Some(fields(4)),
      province = province.map(_.iso31662),
      postal = Some(fields(7)),
      country = Some(country.iso31663),
      latitude = Some(fields(5)),
      longitude = Some(fields(6))
    )
  }

  def ipToDecimal(ip:String): Try[Long] = Try {
    ip.split("\\.").map(_.toInt) match {
      case(Array(a, b, c, d)) => {
        a * scala.math.pow(256, 3).toLong +
          b * scala.math.pow(256, 2).toLong +
          c * scala.math.pow(256, 1).toLong +
          d * scala.math.pow(256, 0).toLong
      }
      case _ => throw new IllegalArgumentException(s"Unable to parse ip address ${ip}")
    }

  }

  def lookup(ip: Long, index: IndexedSeq[DigitalElementIndexRecord]): Option[DigitalElementIndexRecord] =
      Collections.searchWithBoundary(index, ip)((a,b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)

  /**
    * Very stateful method that builds the index of DigitalElement ip records from an InputStream
    * (mainly because AWS SDK returns an input stream from S3 getObject requests.
    * @param is the input stream to parse
    * @param fieldDelimiter
    * @param recordDelimiter
    * @return an indexed sequence of ip ranges.  Ordering is not changed from which records arrive in the input stream
    */
  def buildIndex(is: InputStream, fieldDelimiter: Char, recordDelimiter: Char): IndexedSeq[DigitalElementIndexRecord] = {
    val rd = recordDelimiter.toByte
    val fd = fieldDelimiter.toByte
    val indexBuilder = IndexedSeq.newBuilder[DigitalElementIndexRecord]
    val recordBuilder = new mutable.ArrayBuilder.ofByte
    var charCounter = -1
    var firstFieldLimit = -1
    var secondFieldLimit = -1
    var cur: Int = 0
    cur = is.read()
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
      cur = is.read()
    }
    indexBuilder.result()
  }


}
