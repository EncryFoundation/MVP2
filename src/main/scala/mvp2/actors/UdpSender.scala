package mvp2.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data.InnerMessages.{MsgToNetwork, SendToNetwork, UdpSocket}
import mvp2.data.NetworkMessages._
import mvp2.utils.{EncodingUtils, Settings, Sha256}

class UdpSender(settings: Settings) extends Actor with StrictLogging {

  val serialization: Serialization = SerializationExtension(context.system)

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case smth: Any => logger.info(s"Got smth strange: $smth.")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case SendToNetwork(message, remote) =>
      logger.info(s"Sending $message to $remote")
      connection ! Udp.Send(serialize(message), remote)
      context.actorSelection("/user/starter/influxActor") !
        MsgToNetwork(
          message,
          Sha256.toSha256(EncodingUtils.encode2Base16(serialize(message)) ++ remote.getAddress.toString),
          remote
        )
  }

  def serialize(message: NetworkMessage): ByteString = ByteString(message match {
    case peers: Peers => NetworkMessagesId.PeersId +: serialization.findSerializerFor(Peers).toBinary(peers)
    case blocks: Blocks => NetworkMessagesId.BlocksId +: serialization.findSerializerFor(blocks).toBinary(blocks)
    case syncIterators: SyncMessageIterators =>
      NetworkMessagesId.SyncMessageIteratorsId +: serialization.findSerializerFor(syncIterators).toBinary(syncIterators)
    case lastBlockHeight: LastBlockHeight =>
      NetworkMessagesId.LastBlockHeightId +: serialization.findSerializerFor(lastBlockHeight).toBinary(lastBlockHeight)
    case transactions: Transactions =>
      NetworkMessagesId.TransactionsId +: serialization.findSerializerFor(transactions).toBinary(transactions)
  })
}