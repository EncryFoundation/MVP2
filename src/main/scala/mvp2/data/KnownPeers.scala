package mvp2.data

import java.net.{InetAddress, InetSocketAddress}
import akka.util.ByteString
import mvp2.data.InnerMessages.SendToNetwork
import mvp2.data.KnownPeers.KnownPeerInfo
import mvp2.data.NetworkMessages.{Blocks, LastBlockHeight, Peers, Transactions}
import mvp2.utils.Settings

case class KnownPeers(var peersPublicKeyMap: Map[InetSocketAddress, KnownPeerInfo],
                      settings: Settings) {


  def updatePeers(peersMsg: Peers, myAddr: InetSocketAddress): KnownPeers =
    peersMsg.peers.foldLeft(this) {
      case (newPeers, nextPeer) => newPeers
        .addOrUpdatePeer(nextPeer.addr, nextPeer.publicKey)
        .updatePeerTime(nextPeer.addr)
        .checkPeersIdentity(peersMsg, myAddr)
  }

  def checkPeersIdentity(peersMsg: Peers, myAddr: InetSocketAddress): KnownPeers =
    this.copy(peersPublicKeyMap +
      (peersMsg.remote ->
        peersPublicKeyMap.getOrElse(peersMsg.remote, KnownPeerInfo())
          .copy(
             knownPeersIdentity = peersMsg
                 .peers
                 .filter(_.addr != myAddr)
                 .forall(peerInfo => peersPublicKeyMap.contains(peerInfo.addr))
         )
        )
    )

  def addOrUpdatePeer(peer: (InetSocketAddress, ByteString)): KnownPeers =
    if (!isSelfIp(peer._1))
      this.copy(peersPublicKeyMap + (
        peer._1 -> peersPublicKeyMap.getOrElse(peer._1, KnownPeerInfo()).copy(Some(peer._2))))
    else this

  def updatePeerTime(peer: InetSocketAddress): KnownPeers =
    if (!isSelfIp(peer))
      this.copy(
        peersPublicKeyMap + (peer ->
          peersPublicKeyMap
            .getOrElse(peer, KnownPeerInfo())
            .copy(lastResponseTime = System.currentTimeMillis()))
      )
    else this

  def getPeersMessages(myAddr: InetSocketAddress, publicKey: ByteString): Seq[SendToNetwork] =
    peersPublicKeyMap.map(peer =>
        SendToNetwork(
          Peers(
            peersPublicKeyMap.flatMap(peer => peer._2.publicKey.map(key => peer._1 -> key)),
            (myAddr, publicKey),
            peer._1
          ),
          peer._1
        )
    ).toSeq

  def getBlockMessage(block: KeyBlock): Seq[SendToNetwork] =
    peersPublicKeyMap.map(peer => SendToNetwork(Blocks(List(block)), peer._1)).toSeq

  def getTransactionMsg(transaction: Transaction): Seq[SendToNetwork] =
    peersPublicKeyMap.map(peer => SendToNetwork(Transactions(List(transaction)), peer._1)).toSeq

  def getHeightMessage(height: Long): Seq[SendToNetwork] = {
    val peersToSync = peersPublicKeyMap
      .filter(_._2.lastResponseTime < System.currentTimeMillis() - settings.network.heightMessageInterval).keys
    peersPublicKeyMap = peersToSync.foldLeft(this) {
      case (newPeers, peer) => newPeers.updatePeerTime(peer)
    }.peersPublicKeyMap
    peersToSync.map(peer => SendToNetwork(LastBlockHeight(height), peer)).toSeq
  }

  def isSelfIp(addr: InetSocketAddress): Boolean =
    (InetAddress.getLocalHost.getAddress sameElements addr.getAddress.getAddress) ||
      (InetAddress.getLoopbackAddress.getAddress sameElements addr.getAddress.getAddress)
}

object KnownPeers {

  case class KnownPeerInfo(publicKey: Option[ByteString],
                           lastResponseTime: Long,
                           knownPeersIdentity: Boolean)

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