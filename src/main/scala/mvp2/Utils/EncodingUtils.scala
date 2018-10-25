package mvp2.Utils

import akka.util.ByteString
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {
  implicit val byteStringEncoder: Encoder[ByteString] = _.utf8String.asJson
  implicit val byteStringDecoder: Decoder[ByteString] = Decoder.decodeString.map(str => ByteString(str))
}