package mvp2.utils

import java.util.Base64
import akka.util.ByteString
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {
  implicit val byteStringEncoder: Encoder[ByteString] = _.utf8String.asJson
  implicit val byteStringDecoder: Decoder[ByteString] = Decoder.decodeString.map(str => ByteString(str))

  private val encoder = Base64.getEncoder

  private val decoder = Base64.getDecoder

  def encode2Base64(bytes: ByteString): String = new String(encoder.encode(bytes.toArray))

  def decodeFromBase64(base64Str: String): ByteString = ByteString(decoder.decode(base64Str))
}