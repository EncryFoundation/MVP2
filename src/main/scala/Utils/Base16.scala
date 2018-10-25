package Utils

import akka.util.ByteString
import scala.util.Try

object Base16 {
  val alphabet: String = "0123456789abcdefABCDEF"
  private val hexArray = "0123456789abcdef".toCharArray

  private val hexIndex: ByteString = {
    val index = Array.fill[Byte](128)(0xff.toByte)
    hexArray.zipWithIndex.foreach { case (c, i) =>
      index(c) = i.toByte
    }
    "abcdef".toCharArray.foreach { c =>
      index(c.toUpper) = index(c)
    }
    ByteString(index)
  }

  def encode(input: ByteString): String = {
    val buf = new Array[Char](input.length * 2)
    input.indices.foreach { j =>
      val v = input(j) & 0xFF
      buf(j * 2) = hexArray(v >>> 4)
      buf(j * 2 + 1)= hexArray(v & 0x0F)
    }
    new String(buf)
  }

  def decode(input: String): Try[ByteString] =
    Try {
      if (input.length %2 != 0) throw new RuntimeException(s"invalid length ${input.length} of Hex data")
      val out: Array[Byte] = (0 until input.length by 2).foldLeft(Array.ofDim[Byte](input.length / 2)) {
        case (acc, j) =>
          val c1 = input(j)
          val c2 = input(j + 1)
          if (c1 > 0 && c1 < 127 && c2 > 0 && c2 < 127) {
            val b1 = hexIndex(c1)
            val b2 = hexIndex(c2)
            if ((b1 | b2) < 0) throw new RuntimeException("invalid characters encountered in Hex data")
            acc (j / 2) = ((b1 << 4) | b2).toByte
            acc
          } else throw new RuntimeException(s"invalid characters encountered in Hex data")
      }
      ByteString(out)
    }
}