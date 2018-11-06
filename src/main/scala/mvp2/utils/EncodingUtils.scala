package mvp2.utils

import akka.util.ByteString
import com.google.common.io.BaseEncoding
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {
  implicit val byteStringEncoder: Encoder[ByteString] = _.utf8String.asJson
  implicit val byteStringDecoder: Decoder[ByteString] = Decoder.decodeString.map(str => ByteString(str))

  def encode2Base16(bytes: ByteString): String = BaseEncoding.base16().encode(bytes.toArray)

  def decodeFromBase16(base16Str: String): ByteString =
    ByteString(BaseEncoding.base16().decode(base16Str))
}