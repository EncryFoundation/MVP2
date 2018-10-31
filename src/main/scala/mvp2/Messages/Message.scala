package mvp2.Messages

import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.Data.Block

sealed trait Message

case object Get extends Message

final case class InfoMessage(info: String) extends Message

case class CurrentBlockchainInfo(height: Int,
                                 lastGeneralBlock: Option[ByteString],
                                 lastMicroBlock: Option[ByteString]) extends Message

sealed trait PingPong extends Message

case object Ping extends PingPong

case object Pong extends PingPong

case class UdpSocket(conection: ActorRef) extends Message

case class SendBlockToState(block: Block) extends Message

case object GetCurrentInfoFromBlockchainer extends Message