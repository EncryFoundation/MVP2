package mvp2.utils

import java.util.Base64
import akka.util.ByteString
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {

  private val encoder = Base64.getEncoder

  private val decoder = Base64.getDecoder

  implicit val byteStringEncoder: Encoder[ByteString] = byteString => encode2Base64(byteString).asJson
  implicit val byteStringDecoder: Decoder[ByteString] = Decoder.decodeString.map(str => decodeFromBase64(str))

  def encode2Base64(bytes: ByteString): String = new String(encoder.encode(bytes.toArray))

  def decodeFromBase64(base64Str: String): ByteString = ByteString(decoder.decode(base64Str))
}