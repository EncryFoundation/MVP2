package mvp2.messages

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.data.{Block, KeyBlock, Transaction}

sealed trait Message

case object Get extends Message

final case class InfoMessage(info: String) extends Message

final case class CurrentBlockchainInfo(height: Long = 0,
                                       lastKeyBlock: Option[KeyBlock] = None,
                                       lastMicroBlock: Option[ByteString] = None) extends Message {

  override def toString: String = s"Height: $height, last keyBlock: $lastKeyBlock, last microBlock: $lastMicroBlock."
}

final case class NewPublisher(publicKey: ByteString) extends Message

sealed trait NetworkMessage extends Message

case object Ping extends NetworkMessage {

  val typeId: Byte = 1: Byte
}

case object Pong extends NetworkMessage {

  val typeId: Byte = 2: Byte
}

case class Peers(peers: List[InetSocketAddress], remote: InetSocketAddress) extends NetworkMessage

case object Peers {

  val typeId: Byte = 3: Byte
}

case class Blocks(chain: List[Block]) extends NetworkMessage

object Blocks {

  val typeId: Byte = 4: Byte
}

case class SyncMessageIterators(iterators: Map[String, Int]) extends NetworkMessage

object SyncMessageIterators {

  val typeId: Byte = 5: Byte
}

case class Transactions(transactions: List[Transaction]) extends NetworkMessage

object Transactions {

  val typeId: Byte = 6
}

case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class MsgToNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends Message

case class MsgFromNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends Message

case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class SyncMessageIteratorsFromRemote(iterators: Map[String, Int], remote: InetSocketAddress) extends Message

case class UdpSocket(conection: ActorRef) extends Message

case class TimeDelta(delta: Long)

case object GetLightChain extends Message

case class TransactionFromRemote(tx: Transaction) extends Message