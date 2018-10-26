package mvp2.Data

import akka.util.ByteString

sealed trait Block {
  val height: Int

  def isValid: Boolean
}

final case class KeyBlock(override val height: Int,
                          previousGeneralBlock: ByteString,
                          data: ByteString) extends Block {
  override def isValid: Boolean = ???
}

final case class MicroBlock(override val height: Int,
                            previousGeneralBlock: ByteString,
                            previousMiniBlock: ByteString,
                            transactions: List[Transaction],
                            data: ByteString) extends Block {
  override def isValid: Boolean = ???
}