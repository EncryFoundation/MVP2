package mvp2.data

import akka.util.ByteString
import mvp2.utils.Sha256
import mvp2.utils.EncodingUtils._

sealed trait Block {
  val height: Long
  val timestamp: Long
  val previousKeyBlockHash: ByteString
  val currentBlockHash: ByteString

  def isValid: Boolean

  override def toString: String = s"Height: $height, time = $timestamp, " +
    s"previousKeyBlockHash = ${encode2Base64(previousKeyBlockHash)}, " +
    s"currentBlockHash = ${encode2Base64(currentBlockHash)}."
}

final case class KeyBlock(height: Long,
                          timestamp: Long,
                          previousKeyBlockHash: ByteString,
                          currentBlockHash: ByteString,
                          transactions: List[Transaction],
                          data: ByteString) extends Block {
  override def isValid: Boolean = true//height >= 0 && timestamp > System.currentTimeMillis() - 15000

}

object KeyBlock {
  def apply(height: Long = -1,
            timestamp: Long = System.currentTimeMillis,
            previousKeyBlockHash: ByteString = ByteString.empty,
            transactions: List[Transaction] = List.empty,
            data: ByteString = ByteString.empty): KeyBlock = {
    val currentBlockHash: ByteString = Sha256.toSha256(height.toString + timestamp.toString + previousKeyBlockHash.toString)
    new KeyBlock(height, timestamp, previousKeyBlockHash, currentBlockHash, transactions, data)
  }
}

final case class MicroBlock(height: Long,
                            timestamp: Long,
                            previousKeyBlockHash: ByteString = ByteString.empty,
                            previousMicroBlock: ByteString,
                            currentBlockHash: ByteString,
                            transactions: List[Transaction] = List.empty,
                            data: ByteString = ByteString.empty) extends Block {
  override def isValid: Boolean = true
}