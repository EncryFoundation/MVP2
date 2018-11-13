package mvp2.messages

import java.net.InetSocketAddress
import java.security.PublicKey
import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.data.{Block, KeyBlock}

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

case object Ping extends NetworkMessage

case object Pong extends NetworkMessage

case class Peers(peers: Map[InetSocketAddress, Option[ByteString]], remote: InetSocketAddress) extends NetworkMessage

case object Peers {

  def apply(peers: Map[InetSocketAddress, Option[ByteString]],
            myNode: (InetSocketAddress, Option[ByteString]),
            remote: InetSocketAddress): Peers =
    Peers(peers.filter(_._1 != remote) + myNode, remote)
}

case class Blocks(blocks: List[Block]) extends NetworkMessage

object NetworkMessagesId {

  val PingId: Byte = 1
  val PongId: Byte = 2
  val PeersId: Byte = 3
  val BlocksId: Byte = 4
}

case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class UdpSocket(conection: ActorRef) extends Message

case class PeerPublicKey(peerPublicKey: PublicKey) extends Message

case class MyPublicKey(publicKey: PublicKey) extends Message

case object GetLightChain extends Message