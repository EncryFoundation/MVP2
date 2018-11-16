package mvp2.data

import java.net.InetSocketAddress
import akka.util.ByteString
import mvp2.utils.EncodingUtils

object NetworkMessages {

  object NetworkMessagesId {
    val PeersId: Byte = 1
    val BlocksId: Byte = 2
    val SyncMessageIteratorsId: Byte = 3
  }

  sealed trait NetworkMessage

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

  case class SyncMessageIterators(iterators: Map[String, Int]) extends NetworkMessage

}