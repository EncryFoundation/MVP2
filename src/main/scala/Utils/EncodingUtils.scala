package Utils

import akka.util.ByteString
import Utils.Base16.encode
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {
  implicit val byteStringEncoder: Encoder[ByteString] = bytes => encode(bytes).asJson
  implicit val byteStringDecoder: Decoder[ByteString] =
    Decoder.decodeString.map(Base16.decode).map(_.getOrElse(ByteString.empty))
}