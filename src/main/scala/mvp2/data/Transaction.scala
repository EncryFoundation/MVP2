package mvp2.data

import akka.util.ByteString
import com.google.protobuf.{ByteString => pByteString}
import mvp2.data.my_messages.TransactionProtobuf
import mvp2.utils.EncodingUtils.encode2Base16

case class Transaction(publicKey: ByteString,
                       timestamp: Long = System.currentTimeMillis,
                       nonce: Long,
                       signature: ByteString,
                       data: ByteString) {
  def isValid: Boolean = true

  override def toString: String = s"PublicKey: ${encode2Base16(publicKey)}, timestamp = $timestamp, nonce = $nonce, " +
    s"signature = ${encode2Base16(signature)}."
}

object Transaction {

  def toProtobuf(tx: Transaction): TransactionProtobuf = TransactionProtobuf()
    .withNonce(tx.nonce)
    .withPublicKey(pByteString.copyFrom(tx.publicKey.toByteBuffer))
    .withSignature(pByteString.copyFrom(tx.signature.toByteBuffer))
    .withTimestamp(tx.timestamp)
    .withData(pByteString.copyFrom(tx.data.toByteBuffer))

  def fromProtobuf(txProt: TransactionProtobuf): Transaction = Transaction(
    ByteString(txProt.publicKey.toByteArray),
    txProt.timestamp,
    txProt.nonce,
    ByteString(txProt.signature.toByteArray),
    ByteString(txProt.data.toByteArray)
  )
}
