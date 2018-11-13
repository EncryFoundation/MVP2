package mvp2.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.messages._
import mvp2.utils.Settings

class Sender(settings: Settings) extends Actor with StrictLogging {

  val serialization: Serialization = SerializationExtension(context.system)

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case smth: Any => logger.info(s"Got smth strange: $smth.")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case SendToNetwork(message, remote) =>
      logger.info(s"Send $message to $remote")
      connection ! Udp.Send(serialize(message), remote)
      if (settings.influx.isDefined)
        context.actorSelection("/user/starter/influxActor") ! SendToNetwork(message, remote)
  }

  def serialize(message: NetworkMessage): ByteString = ByteString(message match {
    case ping: Ping.type => NetworkMessagesId.PingId +: serialization.findSerializerFor(Ping).toBinary(ping)
    case pong: Pong.type => NetworkMessagesId.PongId +: serialization.findSerializerFor(Pong).toBinary(pong)
    case peers: Peers => NetworkMessagesId.PeersId +: serialization.findSerializerFor(Peers).toBinary(peers)
    case blocks: Blocks => NetworkMessagesId.BlocksId +: serialization.findSerializerFor(blocks).toBinary(blocks)
  })
}