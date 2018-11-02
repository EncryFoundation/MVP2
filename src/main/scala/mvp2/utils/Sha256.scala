package mvp2.utils

import java.security.MessageDigest
import akka.util.ByteString

object Sha256 {

  val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

  def toSha256(bytes: ByteString): ByteString = ByteString(digest.digest(bytes.toArray))
}