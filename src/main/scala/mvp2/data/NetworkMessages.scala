package mvp2.data

import java.net.InetSocketAddress
import com.google.protobuf.{ByteString => pByteString}
import akka.util.ByteString
import mvp2.data.my_messages._
import mvp2.utils.EncodingUtils
import scala.util.Try

object NetworkMessages {

  case class PeerInfo(addr: InetSocketAddress, publicKey: ByteString)

  object PeerInfo {

    def toProtobuf(peer: PeerInfo): PeerProtobuf = PeerProtobuf()
      .withAddress(EncodingUtils.fromISA2Str(peer.addr))
      .withPublickey(pByteString.copyFrom(peer.publicKey.toByteBuffer))

    def fromProtobuf(peerProt: PeerProtobuf): PeerInfo = PeerInfo(
      EncodingUtils.fromStr2ISA(peerProt.address),
      ByteString(peerProt.publickey.toByteArray)
    )
  }

  case class IterInfo(msgName: String, msgIter: Int)

  object IterInfo {

    def toProtobuf(iter: IterInfo): IterInfoProtobuf = IterInfoProtobuf()
      .withIter(iter.msgIter)
      .withMsgName(iter.msgName)

    def fromProtobuf(iterProt: IterInfoProtobuf): IterInfo =
      IterInfo(iterProt.msgName, iterProt.iter)
  }

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

    def toBytes(peers: Peers): ByteString = ByteString(PeersProtobuf()
      .withPeers(peers.peers.map(PeerInfo.toProtobuf))
      .withRemote(EncodingUtils.fromISA2Str(peers.remote))
      .toByteArray)

    def parseBytes(bytes: ByteString): Try[Peers] = Try {
      val peersProt: PeersProtobuf = PeersProtobuf.parseFrom(bytes.toArray)
      Peers(
        peersProt.peers.map(PeerInfo.fromProtobuf).toList,
        EncodingUtils.fromStr2ISA(peersProt.remote)
      )
    }
  }

  case class Blocks(chain: List[KeyBlock]) extends NetworkMessage {

    override val name: String = "blocks"
  }

  object Blocks {

    def toBytes(blocks: Blocks): ByteString = ByteString(BlocksProtobuf()
      .withBlocks(blocks.chain.map(KeyBlock.toProtobuf))
      .toByteArray)

    def parseBytes(bytes: ByteString): Try[Blocks] = Try {
      val blocksProt = BlocksProtobuf.parseFrom(bytes.toArray)
      Blocks(blocksProt.blocks.map(KeyBlock.fromProtobuf).toList)
    }
  }

  case class SyncMessageIterators(iterators: List[IterInfo]) extends NetworkMessage {

    override val name: String = "iterators"
  }

  object SyncMessageIterators {

    def toBytes(iteratorsMsg: SyncMessageIterators): ByteString = ByteString(SyncMessageIteratorsProtobuf()
      .withIters(iteratorsMsg.iterators.map(IterInfo.toProtobuf))
      .toByteArray)

    def parseBytes(bytes: ByteString): Try[SyncMessageIterators] = Try {
      val smiProto = SyncMessageIteratorsProtobuf.parseFrom(bytes.toArray)
      SyncMessageIterators(smiProto.iters.map(IterInfo.fromProtobuf).toList)
    }
  }

  case class Transactions(transactions: List[Transaction]) extends NetworkMessage {

    override val name: String = "tx"
  }

  object Transactions {

    def toBytes(txsMsg: Transactions): ByteString = ByteString(TransactionsProtobuf()
      .withTxs(txsMsg.transactions.map(Transaction.toProtobuf))
      .toByteArray)

    def parseBytes(bytes: ByteString): Try[NetworkMessage] = Try {
      val txProto = TransactionsProtobuf.parseFrom(bytes.toArray)
      Transactions(txProto.txs.map(Transaction.fromProtobuf).toList)
    }
  }

  case class LastBlockHeight(height: Long) extends NetworkMessage {

    override val name: String = "lastBlockHeight"
  }

  object LastBlockHeight {

    def toBytes(lbhMsg: LastBlockHeight): ByteString = ByteString(LastBlockHeightProtobuf()
      .withHeight(lbhMsg.height)
      .toByteArray)

    def parseBytes(bytes: ByteString): Try[LastBlockHeight] = Try {
      LastBlockHeight(LastBlockHeightProtobuf.parseFrom(bytes.toArray).height)
    }
  }

}
