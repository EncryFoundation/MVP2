package mvp2.messages

import java.net.InetSocketAddress
import java.security.PublicKey
import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.data.{Block, KeyBlock}
import mvp2.utils.EncodingUtils

sealed trait Message

case object Get extends Message

final case class CurrentBlockchainInfo(height: Long = 0,
                                       lastKeyBlock: Option[KeyBlock] = None,
                                       lastMicroBlock: Option[ByteString] = None) extends Message {

  override def toString: String = s"Height: $height, last keyBlock: $lastKeyBlock, last microBlock: $lastMicroBlock."
}

final case class NewPublisher(publicKey: ByteString) extends Message

sealed trait NetworkMessage extends Message

case class Peers(peers: Map[InetSocketAddress, ByteString], remote: InetSocketAddress) extends NetworkMessage {

  override def toString: String =
    peers.map(peerInfo => s"${peerInfo._1} -> ${EncodingUtils.encode2Base16(peerInfo._2)}").mkString(",")
}

case object Peers {

  def apply(peers: Map[InetSocketAddress, ByteString],
            myNode: (InetSocketAddress, ByteString),
            remote: InetSocketAddress): Peers =
    Peers(peers.filter(_._1 != remote) + myNode, remote)
}

case class Blocks(chain: List[Block]) extends NetworkMessage

object NetworkMessagesId {

  val PeersId: Byte = 1
  val BlocksId: Byte = 2
  val SyncMessageIteratorsId: Byte = 3
}

case class SyncMessageIterators(iterators: Map[String, Int]) extends NetworkMessage

case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class MsgToNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends Message

case class MsgFromNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends Message

case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class SyncMessageIteratorsFromRemote(iterators: Map[String, Int], remote: InetSocketAddress) extends Message

case class UdpSocket(conection: ActorRef) extends Message

case class PeerPublicKey(peerPublicKey: PublicKey) extends Message

case class MyPublicKey(publicKey: PublicKey) extends Message

case class TimeDelta(delta: Long)

case object GetLightChain extends Message