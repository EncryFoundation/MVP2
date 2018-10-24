package Actors

import java.net.InetSocketAddress
import Actors.Sender.{Ping, Pong, UdpSocket}
import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Sender extends Actor with StrictLogging {

  val remote = new InetSocketAddress("localhost", 5678)

  override def preStart(): Unit = {
    context.system.scheduler.schedule(1.seconds, 1.seconds)(self ! Ping)
  }

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case msg => logger.info(s"Smth strange: $msg")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case Ping =>
      connection ! Udp.Send(ByteString("Ping"), remote)
      logger.info(s"Send ping to: $connection")
    case Pong =>
      connection ! Udp.Send(ByteString("Pong"), remote)
      logger.info(s"Send pong to remote: $connection")
  }
}

object Sender {

  case object Ping

  case object Pong

  case class UdpSocket(conection: ActorRef)
}
