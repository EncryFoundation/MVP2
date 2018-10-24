package Actors

import Actors.Sender.{Ping, Pong, RemoteConnection}
import MVP2.MVP2._
import akka.actor.{Actor, ActorRef}
import akka.io.UdpConnected
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

class Sender extends Actor with StrictLogging {

  override def preStart(): Unit = {
    context.system.scheduler.schedule(1.seconds, 1.seconds)(self ! Ping)
  }

  override def receive: Receive = {
    case RemoteConnection(connection) => context.become(sendingCycle(connection))
    case msg => logger.info(s"Smth strange: $msg")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case Ping =>
      connection ! UdpConnected.Send(ByteString("Ping".getBytes))
      logger.info(s"Send ping to: $connection")
    case Pong =>
      connection ! UdpConnected.Send(ByteString("Pong".getBytes))
      logger.info(s"Send pong to remote: $connection")
  }
}

object Sender {

  case object Ping

  case object Pong

  case class RemoteConnection(conection: ActorRef)
}
