package tools

import java.nio.MappedByteBuffer

import utils.DigitalElementOffset

import scala.collection.mutable

case class Offset(start_range: Long, end_range: Long, offset: Int, length: Int)

case class Record(
  start_range: Long,
  end_range: Long,


object Index {

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
