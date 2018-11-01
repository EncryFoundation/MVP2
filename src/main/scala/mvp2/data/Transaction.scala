package mvp2.data

import akka.util.ByteString

case class Transaction(publicKey: ByteString,
                       nonce: Long,
                       signature: ByteString,
                       data: ByteString) {
  def isValid: Boolean = true //FIXME change in later version
}
