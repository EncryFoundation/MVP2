package Actors

import Messages._
import Utils.MessagesSerializer
import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import com.typesafe.scalalogging.StrictLogging

class Sender extends Actor with StrictLogging {

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
