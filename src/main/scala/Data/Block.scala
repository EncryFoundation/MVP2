package Data

import akka.util.ByteString

sealed trait Block {
  def isValid: Boolean
}

final case class GeneralBlock(height: Int,
                              previousGeneralBlock: ByteString,
                              data: ByteString) extends Block {
  override def isValid: Boolean = ???
}

final case class MiniBlock(height: Int,
                           previousGeneralBlock: ByteString,
                           previousMiniBlock: ByteString,
                           data: ByteString) extends Block {
  override def isValid: Boolean = ???
}