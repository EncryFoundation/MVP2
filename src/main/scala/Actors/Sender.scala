package Actors

import java.net.InetSocketAddress

import Messages.{KnownPeers, Ping, Pong, UdpSocket}
import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.serialization._

class Sender extends Actor with StrictLogging {

  import context.system

  override def preStart(): Unit = {
    logger.info("Start sender")
  }

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case msg => logger.info(s"Smth strange: $msg")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case Ping(remote: InetSocketAddress) =>
      connection ! Udp.Send(ByteString("Ping"), remote)
      logger.info(s"Send ping to: $connection")
    case Pong(remote: InetSocketAddress) =>
      connection ! Udp.Send(ByteString("Pong"), remote)
      logger.info(s"Send pong to remote: $connection")
    case KnownPeers(peers, remote) =>
      logger.info(s"Sending: $peers")
      val serialization = SerializationExtension(system)
      val serializer = serialization.findSerializerFor(peers)
      connection ! Udp.Send(ByteString((2:Byte) +: serializer.toBinary(KnownPeers(peers, remote))), remote)
  }
}
