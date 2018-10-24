package Actors

import java.net.InetSocketAddress
import Messages.{Pong, UdpSocket}
import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

class Receiver extends Actor with StrictLogging {

  import context.system

  override def preStart(): Unit = {
    logger.info("Start receiver")
    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 1234))
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
      logger.info(s"Received ${data.utf8String} from $remote")
      data.utf8String match {
        case "Ping" =>
          logger.info(s"Get ping from: $remote send Pong")
          context.actorSelection("/user/starter/networker/sender") ! Pong
        case "Pong" =>
          logger.info(s"Get pong from: $remote send Pong")
      }
    case Udp.Unbind  =>
      socket ! Udp.Unbind
      logger.info(s"Unbind $socket")
    case Udp.Unbound =>
      logger.info(s"Unbound $socket")
      context.stop(self)
  }
}
