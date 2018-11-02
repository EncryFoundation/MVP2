package mvp2.data

import java.security.PrivateKey

import akka.util.ByteString
import com.google.common.primitives.Longs
import mvp2.utils.{BlockchainUtils, ECDSA, Sha256}
import mvp2.utils.EncodingUtils._

sealed trait Block {
  val height: Long
  val timestamp: Long
  val previousKeyBlockHash: ByteString
  val hash: ByteString
  val signature: ByteString
  val transactions: List[Transaction]
  val merkleTree: ByteString = BlockchainUtils.merkleTree(transactions.map(_.getHash))

  def isValid: Boolean

  def sign(privateKey: PrivateKey): Block

  override def toString: String = s"Height: $height, time = $timestamp, " +
    s"previousKeyBlockHash = ${encode2Base64(previousKeyBlockHash)}, " + s"currentBlockHash = ${encode2Base64(hash)}."//TODO
}

final case class KeyBlock(height: Long,
                          timestamp: Long,
                          previousKeyBlockHash: ByteString,
                          transactions: List[Transaction],
                          data: ByteString = ByteString.empty,
                          override val signature: ByteString = ByteString.empty) extends Block {

  override val hash: ByteString = Sha256.toSha256(
    ByteString(Longs.toByteArray(height) ++ Longs.toByteArray(timestamp)) ++
      previousKeyBlockHash ++
      merkleTree ++
      data ++
      signature
  )

  override def sign(privateKey: PrivateKey): KeyBlock =
    this.copy(signature = ECDSA.sign(
        privateKey,
        Sha256.toSha256(
          ByteString(Longs.toByteArray(height) ++ Longs.toByteArray(timestamp)) ++
            previousKeyBlockHash ++
            data
        )
      )
    )

  override def isValid: Boolean = {//TODO
    height > 0 && timestamp > System.currentTimeMillis() - 15000
  }
}

final case class MicroBlock(height: Long,
                            timestamp: Long,
                            previousKeyBlockHash: ByteString = ByteString.empty,
                            previousMicroBlock: ByteString,
                            hash: ByteString,
                            transactions: List[Transaction] = List.empty,
                            data: ByteString = ByteString.empty,
                            override val signature: ByteString = ByteString.empty) extends Block {

  override def sign(privateKey: PrivateKey): Block = this

  override def isValid: Boolean = true
}