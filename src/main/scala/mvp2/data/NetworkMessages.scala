package mvp2.data

import java.net.InetSocketAddress
import akka.util.ByteString
import mvp2.utils.EncodingUtils

object NetworkMessages {

  case class PeerInfo(addr: InetSocketAddress, publicKey: ByteString)

  case class IterInfo(msgName: String, msgIter: Int)

  object NetworkMessagesId {
    val PeersId: Byte = 1
    val BlocksId: Byte = 2
    val SyncMessageIteratorsId: Byte = 3
    val TransactionsId: Byte = 4
    val LastBlockHeightId: Byte = 5
  }

  sealed trait NetworkMessage {

    val name: String
  }

  case class Peers(peers: List[PeerInfo], remote: InetSocketAddress) extends NetworkMessage {

    override val name: String = "peers"

    override def toString: String =
      peers.map(peerInfo => s"${peerInfo.addr} -> ${EncodingUtils.encode2Base16(peerInfo.publicKey)}").mkString(",")
  }

  case object Peers {
    def apply(peers: Map[InetSocketAddress, ByteString],
              myNode: (InetSocketAddress, ByteString),
              remote: InetSocketAddress): Peers =
      Peers((peers.filter(_._1 != remote) + myNode).map(peer => PeerInfo(peer._1, peer._2)).toList, remote)
  }

  case class Blocks(chain: List[KeyBlock]) extends NetworkMessage {

    override val name: String = "blocks"
  }

  case class SyncMessageIterators(iterators: List[IterInfo]) extends NetworkMessage {

    override val name: String = "iterators"
  }

  case class Transactions(transactions: List[Transaction]) extends NetworkMessage {

    override val name: String = "tx"
  }

  case class LastBlockHeight(height: Long) extends NetworkMessage {

    override val name: String = "lastBlockHeight"
  }
}
