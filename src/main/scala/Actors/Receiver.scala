package Actors

import java.net.InetSocketAddress

import Actors.Sender.Pong
import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp, UdpConnected}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

class Receiver extends Actor with StrictLogging {

  import context.system

  override def preStart(): Unit = {
    IO(UdpConnected) ! UdpConnected.Connect(self, new InetSocketAddress("localhost", 1234))
  }

  override def receive: Receive = {
    case UdpConnected.Connected =>
      logger.info(s"Bound remote to $sender")
      context.actorSelection("/user/Sender") ! Pong
      context.become(readCycle(sender))
    case msg => logger.warn(s"Received message $msg from $sender before binding")
  }

  def readCycle(remote: ActorRef): Receive = {
    case Udp.Received(data: ByteString, remote: InetSocketAddress) =>
      logger.info(s"Received ${data.toString} from $remote")
      data.toString match {
        case "Ping" =>
          logger.info(s"Get ping from: $remote send Pong")
          context.actorSelection("/user/Sender") ! Pong
        case "Pong" =>
          logger.info(s"Get pong from: $remote send Pong")
      }
    case UdpConnected.Disconnect =>
      logger.info(s"Unbind $remote")
      remote ! UdpConnected.Disconnect
    case UdpConnected.Disconnected =>
      logger.info(s"Unbound $remote")
      context.stop(self)
  }
}
