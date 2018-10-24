package Messages

import akka.actor.ActorRef

sealed trait Message

sealed trait PingPong extends Message

case object Start extends Message

final case class InfoMessage(info: String) extends Message

case object Ping extends PingPong

case object Pong extends PingPong

case class UdpSocket(conection: ActorRef) extends Message