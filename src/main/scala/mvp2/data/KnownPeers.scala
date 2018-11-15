package mvp2.data

import java.net.{InetAddress, InetSocketAddress}
import akka.util.ByteString
import mvp2.messages.{Blocks, Peers, SendToNetwork}
import mvp2.utils.Settings

case class KnownPeers(peersMap: Map[InetSocketAddress, (Long, Option[ByteString])]) {

  def addOrUpdatePeer(peer: (InetSocketAddress, Option[ByteString])): KnownPeers =
    if (!isSelfIp(peer._1))
      KnownPeers((peersMap - peer._1) + (peer._1 -> peersMap.get(peer._1).map(prevPeerInfo =>
        if (prevPeerInfo._2.isEmpty) (prevPeerInfo._1, peer._2)
        else prevPeerInfo
      ).getOrElse((System.currentTimeMillis(), None))))
    else this

  def updatePeerTime(peer: InetSocketAddress): KnownPeers =
    if (!isSelfIp(peer))
      KnownPeers((peersMap - peer) +
        (peer -> peersMap.getOrElse(peer, (System.currentTimeMillis(), None)).copy(_1 = System.currentTimeMillis())))
    else this

  def getPeersMessages(myAddr: InetSocketAddress, publicKey: Option[ByteString]): Seq[SendToNetwork] =
    peersMap.toList.map(peer =>
        SendToNetwork(
          Peers(peersMap.toList.map(peer => (peer._1, peer._2._2)).toMap, (myAddr, publicKey), peer._1),
          peer._1
        )
    )

  def getBlockMsg(block: KeyBlock): Seq[SendToNetwork] =
    peersMap.toList.map(peer =>
        SendToNetwork(Blocks(List(block)), peer._1)
    )

  def isSelfIp(addr: InetSocketAddress): Boolean =
    (InetAddress.getLocalHost.getAddress sameElements addr.getAddress.getAddress) ||
      (InetAddress.getLoopbackAddress.getAddress sameElements addr.getAddress.getAddress)
}

object KnownPeers {

  def apply(settings: Settings): KnownPeers =
    new KnownPeers(settings.otherNodes.map(node =>
      new InetSocketAddress(node.host, node.port) -> (0: Long, None)).toMap
    )
}

case class Peer(remoteAddress: InetSocketAddress,
                lastMessageTime: Long,
                key: Option[ByteString])
