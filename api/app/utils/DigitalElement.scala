package utils
import java.io.InputStream

import io.flow.common.v0.models.Address
import io.flow.reference.v0.models.Province
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
  rangeStart: BigInt,
  rangeEnd: BigInt,
  fieldDelimiter: Char,
  bytes: Array[Byte]) extends Ordered[DigitalElementIndexRecord] {

  override def compare(that: DigitalElementIndexRecord): Int = (this.rangeStart - that.rangeStart).toInt

  /**
    * Constructs an Address from an index record using the format specified
    * for the DigitalElementIndexRecord bytes value
    */
  def toAddress(): Address = {
    val fields = new String(this.bytes).split(this.fieldDelimiter)
    val country = Countries.find(fields(2))
    val province = country.flatMap (c => { Provinces.find(s"${c.iso31663}-${fields(3)}") })
    Address(
      city = Some(fields(4)),
      province = province.map(_.name),
      postal = Some(fields(7)),
      country = country.map(_.iso31663),
      latitude = Some(fields(5)),
      longitude = Some(fields(6))
    )
  }
}

object DigitalElement {

  private[this] val ipv4 = "(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)".r
  // digitalelement separates the network and interface portions of ipv6
  // so we only care about the first 4 groups
  private[this] val ipv6 = "([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*):([a-fA-F0-9]*)((:[a-fA-F0-9]*){4})".r

  /**
    * Handle fully-collapsed ipv6 groups ("z" for "zero" ;) )
    * @param s
    * @return "0" if it was an empty string, identity otherwise
    */
  private[this] def z(s: String) = s match {
    case "" => "0"
    case _ => s
  }

  def ipToDecimal(ip:String): Try[BigInt] = Try {
    ip match {
      case ipv4(a, b, c, d) => {
        a.toInt * scala.math.pow(256, 3).toLong +
          b.toInt * scala.math.pow(256, 2).toLong +
          c.toInt * scala.math.pow(256, 1).toLong +
          d.toInt * scala.math.pow(256, 0).toLong
      }
      case ipv6(a, b, c, d, _*) => {
        Integer.parseInt(z(a), 16) * scala.math.pow(65536, 3).toLong +
          Integer.parseInt(z(b), 16) * scala.math.pow(65536, 2).toLong +
          Integer.parseInt(z(c), 16) * scala.math.pow(65536, 1).toLong +
          Integer.parseInt(z(d), 16) * scala.math.pow(65536, 0).toLong
      }
      case _ => throw new IllegalArgumentException(s"Unable to parse ip address ${ip}")
    }

  }

  /**
    * Very stateful method that builds the index of DigitalElement ip records from an InputStream
    * (mainly because AWS SDK returns an input stream from S3 getObject requests.
    * @param is the input stream to parse
    * @param fieldDelimiter
    * @param recordDelimiter
    * @return an indexed sequence of ip ranges.  Ordering is not changed from which records arrive in the input stream
    */
  def buildIndex(is: InputStream, fieldDelimiter: Char, recordDelimiter: Char): DigitalElementIndex = {
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
          val rangeStart = BigInt(new String(bytes.slice(0,firstFieldLimit)))
          val rangeEnd = BigInt(new String(bytes.slice(firstFieldLimit+1,secondFieldLimit)))
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
