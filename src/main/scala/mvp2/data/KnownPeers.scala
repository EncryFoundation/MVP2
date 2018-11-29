package mvp2.data

import java.net.{InetAddress, InetSocketAddress}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data.KnownPeers.KnownPeerInfo
import mvp2.data.InnerMessages.ToNet
import mvp2.data.NetworkMessages.{Blocks, LastBlockHeight, Peers, Transactions}
import mvp2.utils.{EncodingUtils, Settings}

case class KnownPeers(var peersPublicKeyMap: Map[InetSocketAddress, KnownPeerInfo],
                      settings: Settings) extends StrictLogging {


  def updatePeers(peersMsg: Peers, myAddr: InetSocketAddress, remote: InetSocketAddress): KnownPeers =
    peersMsg.peers.filter(_.addr != myAddr).foldLeft(this) {
      case (newPeers, nextPeer) =>
        newPeers
          .addOrUpdatePeer(nextPeer.addr, nextPeer.publicKey)
          .updatePeerTime(nextPeer.addr)
          .checkPeersIdentity(peersMsg, myAddr, remote)
    }

  def checkPeersIdentity(peersMsg: Peers, myAddr: InetSocketAddress, remote: InetSocketAddress): KnownPeers =
    if (remote != myAddr) {
      this.copy(peersPublicKeyMap +
        (remote ->
          peersPublicKeyMap.getOrElse(remote, KnownPeerInfo())
            .copy(
              knownPeersIdentity = peersMsg
                .peers
                .filter(_.addr != myAddr)
                .forall(peerInfo => peersPublicKeyMap.contains(peerInfo.addr))
                && peersMsg
                .peers
                .count(peer => peer.addr != myAddr &&
                  peer.addr != peersMsg.remote) == peersPublicKeyMap.count(_._1 != peersMsg.remote)
            )
          )
      )
    }
    else this

  def cleanPeersByTime: KnownPeers = this.copy(
      peersPublicKeyMap.filter(_._2.lastResponseTime > System.currentTimeMillis() - settings.blockPeriod)
    )

  def cleanPeersByIdenticalKnownPeers: KnownPeers = this.copy(peersPublicKeyMap.filter(_._2.knownPeersIdentity))

  def getPeersKeys: List[ByteString] = peersPublicKeyMap.flatMap(_._2.publicKey).toList

  def addOrUpdatePeer(peer: (InetSocketAddress, ByteString)): KnownPeers = {
    logger.info(s"Updating peer: $peer")
    if (!isSelfIp(peer._1)) {
      val le = this.copy(peersPublicKeyMap + (
        peer._1 -> peersPublicKeyMap.getOrElse(peer._1, KnownPeerInfo())
          .copy(Some(peer._2), lastResponseTime = System.currentTimeMillis()))
      )
      logger.info(s"Get peer456 ${peer._1}: ${peersPublicKeyMap
        .getOrElse(peer._1, KnownPeerInfo())}")
      le
    }
    else this
  }

  def updatePeerTime(peer: InetSocketAddress): KnownPeers =
    if (!isSelfIp(peer)) {
      this.copy(
        peersPublicKeyMap + (peer ->
          peersPublicKeyMap
            .getOrElse(peer, KnownPeerInfo())
            .copy(lastResponseTime = System.currentTimeMillis()))
      )
    }
    else this

  def getPeersMessages(myAddr: InetSocketAddress, publicKey: ByteString): Seq[ToNet] =
    peersPublicKeyMap.map(peer =>
      ToNet(
          Peers(
            peersPublicKeyMap.flatMap(peer => peer._2.publicKey.map(key => peer._1 -> key)),
            (myAddr, publicKey),
            peer._1
          ),
          peer._1
        )
    ).toSeq

  def getBlockMessage(block: KeyBlock): Seq[ToNet] =
    peersPublicKeyMap.map(peer => ToNet(Blocks(List(block)), peer._1)).toSeq

  def getTransactionMsg(transaction: Transaction): Seq[ToNet] =
    peersPublicKeyMap.map(peer => ToNet(Transactions(List(transaction)), peer._1)).toSeq

  def getHeightMessage(height: Long): Seq[ToNet] = {
    val peersToSync = peersPublicKeyMap
      .filter(_._2.lastResponseTime < System.currentTimeMillis() - settings.network.heightMessageInterval).keys
    peersPublicKeyMap = peersToSync.foldLeft(this) {
      case (newPeers, peer) => newPeers.updatePeerTime(peer)
    }.peersPublicKeyMap
    peersToSync.map(peer => ToNet(LastBlockHeight(height), peer)).toSeq
  }

  def isSelfIp(addr: InetSocketAddress): Boolean =
    (InetAddress.getLocalHost.getAddress sameElements addr.getAddress.getAddress) ||
      (InetAddress.getLoopbackAddress.getAddress sameElements addr.getAddress.getAddress)
}

object KnownPeers {

  case class KnownPeerInfo(publicKey: Option[ByteString],
                           lastResponseTime: Long,
                           knownPeersIdentity: Boolean) {

    override def toString: String = s"(PublicKey: ${publicKey.map(EncodingUtils.encode2Base16)}. " +
      s"lastResponseTime: $lastResponseTime. knownPeersIdentity: $knownPeersIdentity)"
  }

  object KnownPeerInfo {

    def apply(publicKey: Option[ByteString] = None,
              lastResponseTime: Long = System.currentTimeMillis(),
              knownPeersIdentity: Boolean = false): KnownPeerInfo =
      new KnownPeerInfo(publicKey, lastResponseTime, knownPeersIdentity)
  }

  def apply(settings: Settings): KnownPeers =
    new KnownPeers(
      settings.otherNodes.map(node => new InetSocketAddress(node.host, node.port) -> KnownPeerInfo()).toMap,
      settings
    )
}