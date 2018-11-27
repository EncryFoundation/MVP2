package mvp2.data

import akka.util.ByteString
import com.google.common.primitives.Longs
import com.google.protobuf.{ByteString => pByteString}
import mvp2.data.my_messages.KeyBlockProtobuf
import mvp2.utils.Sha256
import mvp2.utils.EncodingUtils._

sealed trait Block {
  val height: Long
  val timestamp: Long
  val previousKeyBlockHash: ByteString
  val currentBlockHash: ByteString

  def isValid(previousBlock: Block): Boolean

  override def toString: String = s"Height: $height, time = $timestamp, " +
    s"previousKeyBlockHash = ${encode2Base16(previousKeyBlockHash)}, " + s"currentBlockHash = ${encode2Base16(currentBlockHash)}."
}

final case class KeyBlock(height: Long,
                          timestamp: Long,
                          previousKeyBlockHash: ByteString,
                          currentBlockHash: ByteString,
                          transactions: List[Transaction],
                          data: ByteString,
                          signature: ByteString,
                          scheduler: List[ByteString]) extends Block with Ordered[KeyBlock] {

  override def isValid(previousBlock: Block): Boolean = previousBlock.height + 1 == this.height

  override def compare(that: KeyBlock): Int = this.height compare that.height

  def getBytes: ByteString = previousKeyBlockHash ++
    currentBlockHash ++
    data ++
    Longs.toByteArray(height) ++
    Longs.toByteArray(timestamp)
}

object KeyBlock {
  def apply(height: Long = -1,
            timestamp: Long = System.currentTimeMillis,
            previousKeyBlockHash: ByteString = ByteString.empty,
            transactions: List[Transaction] = List.empty,
            data: ByteString = ByteString.empty,
            signature: ByteString = ByteString.empty,
            scheduler: List[ByteString] = List.empty): KeyBlock = {
    val currentBlockHash: ByteString = Sha256.toSha256(height.toString + timestamp.toString + previousKeyBlockHash.toString)
    new KeyBlock(height, timestamp, previousKeyBlockHash, currentBlockHash, transactions, data, signature, scheduler)
  }

  def toProtobuf(block: KeyBlock): KeyBlockProtobuf = KeyBlockProtobuf()
    .withCurrentBlockHash(pByteString.copyFrom(block.currentBlockHash.toByteBuffer))
    .withHeight(block.height)
    .withTimestamp(block.timestamp)
    .withTransactions(block.transactions.map(Transaction.toProtobuf))
    .withData(pByteString.copyFrom(block.data.toByteBuffer))
    .withSignature(pByteString.copyFrom(block.signature.toByteBuffer))
    .withScheduler(block.scheduler.map(publicKey => pByteString.copyFrom(publicKey.toByteBuffer)))
    .withPreviousKeyBlockHash(pByteString.copyFrom(block.previousKeyBlockHash.toByteBuffer))

  def fromProtobuf(blockProtobuf: KeyBlockProtobuf): KeyBlock = KeyBlock (
    blockProtobuf.height,
    blockProtobuf.timestamp,
    ByteString(blockProtobuf.previousKeyBlockHash.toByteArray),
    ByteString(blockProtobuf.currentBlockHash.toByteArray),
    blockProtobuf.transactions.map(Transaction.fromProtobuf).toList,
    ByteString(blockProtobuf.data.toByteArray),
    ByteString(blockProtobuf.signature.toByteArray),
    blockProtobuf.scheduler.map(protKey => ByteString(protKey.toByteArray)).toList
  )
}

final case class MicroBlock(height: Long,
                            timestamp: Long,
                            previousKeyBlockHash: ByteString = ByteString.empty,
                            previousMicroBlock: ByteString,
                            currentBlockHash: ByteString,
                            transactions: List[Transaction] = List.empty,
                            data: ByteString = ByteString.empty) extends Block {
  override def isValid(previousBlock: Block): Boolean = true
}

case class LightKeyBlock(height: Long = 0,
                         timestamp: Long = 0,
                         previousKeyBlockHash: ByteString = ByteString.empty,
                         currentBlockHash: ByteString = ByteString.empty,
                         txNum: Int = 0,
                         data: ByteString = ByteString.empty,
                         signature: ByteString = ByteString.empty,
                         scheduler: List[ByteString] = List()) extends Block {
  override def isValid(previousBlock: Block): Boolean = true
}