package mvp2.data

import java.net.{InetAddress, InetSocketAddress}
import akka.util.ByteString
import mvp2.messages.{Blocks, Peers, SendToNetwork}
import mvp2.utils.Settings

case class KnownPeers(peers: List[Peer]) {

  def addOrUpdatePeer(peer: (InetSocketAddress, Option[ByteString])): KnownPeers =
    peers.find(_.remoteAddress == peer._1) match {
      case Some(peerInfo) if peerInfo.key.isEmpty && peer._2.isDefined =>
        this.copy(peers.filter(_ == peerInfo) :+ Peer(peer._1, 0, peer._2))
      case Some(_) => this
      case None if !isSelfIp(peer._1) => this.copy(peers :+ Peer(peer._1, 0, peer._2))
      case None => this
    }

  def updatePeerTime(peer: InetSocketAddress): KnownPeers =
    peers.find(_.remoteAddress == peer).map(prevPeer =>
      this.copy(peers.filter(_ != prevPeer) :+
        prevPeer.copy(lastMessageTime = System.currentTimeMillis(), key = prevPeer.key))
    ).getOrElse(this)

  def getPeersMessages(myAddr: InetSocketAddress, publicKey: Option[ByteString]): Seq[SendToNetwork] =
    peers.map(peer =>
        SendToNetwork(
          Peers(peers.map(peer => (peer.remoteAddress, peer.key)).toMap, (myAddr, publicKey), peer.remoteAddress),
          peer.remoteAddress
        )
    )

  def getBlockMsg(block: KeyBlock): Seq[SendToNetwork] =
    peers.map(peer =>
        SendToNetwork(Blocks(List(block)), peer.remoteAddress)
    )

  def isSelfIp(addr: InetSocketAddress): Boolean =
    (InetAddress.getLocalHost.getAddress sameElements addr.getAddress.getAddress) ||
      (InetAddress.getLoopbackAddress.getAddress sameElements addr.getAddress.getAddress)
}

object KnownPeers {

  def apply(settings: Settings): KnownPeers =
    new KnownPeers(settings.otherNodes.map(node =>
      Peer(new InetSocketAddress(node.host, node.port), System.currentTimeMillis(), None)
    ))
}

case class Peer(remoteAddress: InetSocketAddress,
                lastMessageTime: Long,
                key: Option[ByteString])
