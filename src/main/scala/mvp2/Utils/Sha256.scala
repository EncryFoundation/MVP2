package mvp2.Utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import akka.util.ByteString

object Sha256 {
  val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

  def toSha256(string: String): ByteString = {
    ByteString(digest.digest(string.getBytes(StandardCharsets.UTF_8)))
  }
}

