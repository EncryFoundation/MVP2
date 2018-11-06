package mvp2.data

import akka.util.ByteString
import mvp2.utils.EncodingUtils.encode2Base64

case class Transaction(publicKey: ByteString,
                       timestamp: Long = System.currentTimeMillis,
                       nonce: Long,
                       signature: ByteString,
                       data: ByteString) {
  def isValid: Boolean = true

  override def toString: String = s"PublicKey: ${encode2Base64(publicKey)}, timestamp = $timestamp, nonce = $nonce, " +
    s"signature = ${encode2Base64(signature)}."
}
