package mvp2.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data.InnerMessages.{MsgToNetwork, SendToNetwork, UdpSocket}
import io.circe.syntax._
import io.circe.generic.auto._
import mvp2.utils.EncodingUtils._
import mvp2.utils.{Settings, Sha256}

class UdpSender(settings: Settings) extends Actor with StrictLogging {

  val serialization: Serialization = SerializationExtension(context.system)

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case smth: Any => logger.info(s"Got smth strange: $smth.")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case SendToNetwork(message, remote) =>
      logger.info(s"Sending $message to $remote")
      connection ! Udp.Send(ByteString(message.asJson.toString), remote)
      logger.info(s"Msg size: ${ByteString(message.asJson.toString).length}: ${message.name}")
      context.actorSelection("/user/starter/influxActor") !
        MsgToNetwork(
          message,
          Sha256.toSha256(encode2Base16(ByteString(message.asJson.toString)) ++ remote.getAddress.toString),
          remote
        )
  }
}