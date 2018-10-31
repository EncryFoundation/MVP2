package mvp2.data

import akka.util.ByteString

sealed trait Block {
  val height: Long
  val timestamp: Long
  def isValid: Boolean

  override def toString: String = s"Height: $height, time = $timestamp."
}

final case class KeyBlock(height: Long = 0,
                          timestamp: Long = System.currentTimeMillis,
                          previousGeneralBlock: ByteString = ByteString.empty,
                          transactions: List[Transaction] = List.empty,
                          data: ByteString = ByteString.empty) extends Block {
  override def isValid: Boolean = ???
}

final case class MicroBlock(height: Long,
                            timestamp: Long,
                            previousGeneralBlock: ByteString,
                            previousMiniBlock: ByteString,
                            transactions: List[Transaction],
                            data: ByteString) extends Block {
  override def isValid: Boolean = ???
}