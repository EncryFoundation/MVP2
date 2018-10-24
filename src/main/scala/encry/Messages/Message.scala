package encry.Messages

import akka.actor.ActorRef
import akka.util.ByteString

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
