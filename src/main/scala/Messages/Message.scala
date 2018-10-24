package Messages

import akka.util.ByteString

sealed trait Message

case object Start extends Message

case object Get extends Message

final case class InfoMessage(info: String) extends Message

case class CurrentBlockchainInfo(height: Int,
                                 lastGeneralBlock: Option[ByteString],
                                 lastMicroBlock: Option[ByteString]) extends Message
