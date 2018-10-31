package mvp2.Actors

import java.net.InetSocketAddress
import Utils.MessagesSerializer
import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.Messages.{MessageFromRemote, UdpSocket}
import mvp2.Utils.Settings

class Receiver(settings: Settings) extends Actor with StrictLogging {

  import context.system

  override def preStart(): Unit = {
    logger.info("Start receiver")
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
      MessagesSerializer.deserialize(data.toArray).foreach { message =>
        logger.info(s"Received $message from $remote")
        context.parent ! MessageFromRemote(message, remote)
      }
    case Udp.Unbind  =>
      socket ! Udp.Unbind
      logger.info(s"Unbind $socket")
    case Udp.Unbound =>
      logger.info(s"Unbound $socket")
      context.stop(self)
  }
}
