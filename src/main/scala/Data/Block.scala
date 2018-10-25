package Data

import akka.util.ByteString

sealed trait Block {
  def isValid: Boolean
}

final case class KeyBlock(height: Int,
                          previousGeneralBlock: ByteString,
                          data: ByteString) extends Block {
  override def isValid: Boolean = ???
}

final case class MicroBlock(height: Int,
                            previousGeneralBlock: ByteString,
                            previousMiniBlock: ByteString,
                            transactions: List[Transaction],
                            data: ByteString) extends Block {
  override def isValid: Boolean = ???
}