package mvp2.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data.InnerMessages.{MsgToNetwork, SendToNetwork, UdpSocket}
import io.circe.syntax._
import io.circe.generic.auto._
import mvp2.data.NetworkMessages._
import mvp2.utils.EncodingUtils._
import mvp2.utils.{Settings, Sha256}

class UdpSender(settings: Settings) extends Actor with StrictLogging {

  override def receive: Receive = {
    case UdpSocket(connection) => context.become(sendingCycle(connection))
    case smth: Any => logger.info(s"Got smth strange: $smth.")
  }

  def sendingCycle(connection: ActorRef): Receive = {
    case SendToNetwork(message, remote) =>
      logger.info(s"Sending $message to $remote")
      connection ! Udp.Send(serialize(message), remote)
      logger.info(s"Msg size: ${serialize(message).length}: ${message.name}")
      context.actorSelection("/user/starter/influxActor") !
        MsgToNetwork(
          message,
          Sha256.toSha256(encode2Base16(ByteString(message.asJson.toString)) ++ remote.getAddress.toString),
          remote
        )
  }

  def serialize(message: NetworkMessage): ByteString = message match {
    case peers: Peers => NetworkMessagesId.PeersId +: Peers.toBytes(peers)
    case blocks: Blocks => NetworkMessagesId.BlocksId +: Blocks.toBytes(blocks)
    case syncIterators: SyncMessageIterators =>
      NetworkMessagesId.SyncMessageIteratorsId +: SyncMessageIterators.toBytes(syncIterators)
    case transactions: Transactions =>
      NetworkMessagesId.TransactionsId +: Transactions.toBytes(transactions)
    case lastBlockHeight: LastBlockHeight =>
      NetworkMessagesId.LastBlockHeightId +: LastBlockHeight.toBytes(lastBlockHeight)
  }
}