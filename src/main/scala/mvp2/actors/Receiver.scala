package mvp2.actors

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.MVP2.system
import mvp2.messages._
import mvp2.Utils.Settings

class Receiver(settings: Settings) extends Actor with StrictLogging {

  val serialization: Serialization = SerializationExtension(context.system)

  override def preStart(): Unit = {
    logger.info("Starting the Receiver!")
    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", settings.port))
  }

  override def receive: Receive = {
    case Udp.Bound(local) =>
      logger.info(s"Binded to $local")
      context.become(readCycle(sender))
      context.actorSelection("/user/starter/networker/sender") ! UdpSocket(sender)
    case msg => logger.info(s"Received message $msg from $sender before binding")
  }

  def readCycle(socket: ActorRef): Receive = {
    case Udp.Received(data: ByteString, remote) =>
      deserialize(data).foreach { message =>
        logger.info(s"Received $message from $remote")
        context.parent ! MessageFromRemote(message, remote)
      }
    case Udp.Unbind =>
      socket ! Udp.Unbind
      logger.info(s"Unbind $socket")
    case Udp.Unbound =>
      logger.info(s"Unbound $socket")
      context.stop(self)
  }


  def deserialize(bytes: ByteString): Option[NetworkMessage] = bytes.head match {
    case Ping.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.toArray.tail)).map {
      case ping: Ping.type => ping
    }
    case Pong.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.toArray.tail)).map {
      case pong: Pong.type => pong
    }
    case Peers.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.toArray.tail)).map {
      case knownPeers: Peers => knownPeers
    }
    case Blocks.typeId => Option(serialization.findSerializerFor(Blocks).fromBinary(bytes.toArray.tail)).map {
      case blocks: Blocks => blocks
    }
  }
}