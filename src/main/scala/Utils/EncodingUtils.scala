package Utils

import java.net.InetSocketAddress
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object EncodingUtils {

  implicit val inetSocketAddressEncoder: Encoder[InetSocketAddress] = addr => addr.getHostName.asJson
  implicit val inetSocketAddressDecoder: Decoder[InetSocketAddress] =
    Decoder.decodeString.map(str => new InetSocketAddress(str, 123))
}
