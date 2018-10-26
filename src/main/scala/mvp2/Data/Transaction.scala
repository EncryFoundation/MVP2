package mvp2.Data

import akka.util.ByteString

case class Transaction(publicKey: ByteString,
                       nonce: Long,
                       signature: ByteString,
                       data: Option[ByteString]) {
  def isValid: Boolean = true //TODO
}
