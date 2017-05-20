package tools

import java.io.InputStream
import java.nio.{Buffer, ByteBuffer}

import utils.DigitalElementOffset

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Index {

  def makeIndex(buf: ByteBuffer, fieldDelimiter: Char, recordDelimiter: Char): IndexedSeq[DigitalElementOffset] = {
    val result = ArrayBuffer.empty[DigitalElementOffset]
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
          result += DigitalElementOffset(curRange.toLong, curOffset, buf.position() - curOffset)
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
    result
  }

}
