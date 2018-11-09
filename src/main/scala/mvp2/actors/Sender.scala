package mvp2.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.messages._
import mvp2.utils.{EncodingUtils, Settings, Sha256}

class Sender(settings: Settings) extends Actor with StrictLogging {

  val serialization: Serialization = SerializationExtension(context.system)

  override def preStart(): Unit = logger.info("Starting the Sender!")

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case smth: Any => logger.info(s"Got smth strange: $smth.")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case SendToNetwork(message, remote) =>
      logger.info(s"Send $message to $remote")
      connection ! Udp.Send(serialize(message), remote)
      if (settings.influx.isDefined) {
        context.actorSelection("/user/starter/influxActor") !
          MsgToNetwork(
            message,
            Sha256.toSha256(EncodingUtils.encode2Base16(serialize(message)) ++ remote.getAddress.toString),
            remote
          )
      }
  }

  def serialize(message: NetworkMessage): ByteString = ByteString(message match {
    case ping: Ping.type => Ping.typeId +: serialization.findSerializerFor(Ping).toBinary(ping)
    case pong: Pong.type => Pong.typeId +: serialization.findSerializerFor(Pong).toBinary(pong)
    case knownPeers: Peers => Peers.typeId +: serialization.findSerializerFor(Peers).toBinary(knownPeers)
    case blocks: Blocks => Blocks.typeId +: serialization.findSerializerFor(blocks).toBinary(blocks)
    case syncIterators: SyncMessageIterators =>
      SyncMessageIterators.typeId +: serialization.findSerializerFor(syncIterators).toBinary(syncIterators)
  })
}