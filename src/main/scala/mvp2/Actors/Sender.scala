package mvp2.Actors

import Utils.MessagesSerializer
import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import com.typesafe.scalalogging.StrictLogging
import mvp2.Messages.{SendToNetwork, UdpSocket}

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
      connection ! Udp.Send(MessagesSerializer.serialize(message), remote)
  }
}