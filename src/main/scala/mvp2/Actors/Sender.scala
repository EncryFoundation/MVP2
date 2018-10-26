package mvp2.Actors

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.Messages.{Ping, Pong, UdpSocket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Sender extends Actor with StrictLogging {

  import context.system

  override def preStart(): Unit = logger.info("Start sender")

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case msg => logger.info(s"Smth strange: $msg")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case SendToNetwork(message, remote) =>
      logger.info(s"Send $message to $remote")
      connection ! Udp.Send(MessagesSerializer.toBytes(message), remote)
  }
}