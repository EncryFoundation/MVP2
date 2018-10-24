package Actors

import java.net.InetSocketAddress

import Actors.Sender.{Pong, UdpSocket}
import Messages.{Pong, UdpSocket}
import akka.actor.{Actor, ActorRef}
import akka.io.UdpConnected.Connect
import akka.io.{IO, Udp}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

class Receiver extends Actor with StrictLogging {

  import context.system

  override def preStart(): Unit = {
    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 5678))
  }

  override def receive: Receive = {
    case Udp.Bound(local) =>
      logger.debug(s"Binded to $local")
      context.become(readCycle(sender))
      context.actorSelection("/user/Sender/") ! UdpSocket(sender)
    case msg => logger.warn(s"Received message $msg from $sender before binding")
  }

  def readCycle(socket: ActorRef): Receive = {
    case Udp.Received(data: ByteString, remote) =>
      logger.debug(s"Received ${data.utf8String} from $remote")
      data.utf8String match {
        case "Ping" =>
          logger.debug(s"Get ping from: $remote send Pong")
          context.actorSelection("/user/Sender") ! Pong
        case "Pong" =>
          logger.debug(s"Get pong from: $remote send Pong")
      }
    case Udp.Unbind  =>
      socket ! Udp.Unbind
      logger.debug(s"Unbind $socket")
    case Udp.Unbound =>
      logger.debug(s"Unbound $socket")
      context.stop(self)
  }
}
