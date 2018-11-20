package mvp2.utils

import java.net.InetSocketAddress

import akka.util.ByteString
import com.google.common.io.BaseEncoding
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {
  implicit val byteStringEncoder: Encoder[ByteString] = str => encode2Base16(str).asJson
  implicit val byteStringDecoder: Decoder[ByteString] = Decoder.decodeString.map(decodeFromBase16)
  implicit val inetSocketAddrEncoder: Encoder[InetSocketAddress] = str =>
    s"${str.getAddress} + ${str.getPort}".asJson
  implicit val inetSocketAddrDecoder: Decoder[InetSocketAddress] = str => Decoder.decodeString(str).map{ addrStr =>
    val split = addrStr.split(":")
    new InetSocketAddress(split(0), split(1).toInt)
  }

  def encode2Base16(bytes: ByteString): String = BaseEncoding.base16().encode(bytes.toArray)

  def decodeFromBase16(base16Str: String): ByteString =
    ByteString(BaseEncoding.base16().decode(base16Str))
}