package mvp2.data

import java.net.{InetAddress, InetSocketAddress}
import akka.util.ByteString
import mvp2.data.InnerMessages.SendToNetwork
import mvp2.data.NetworkMessages.{Blocks, LastBlockHeight, Peers, Transactions}
import mvp2.utils.Settings

case class KnownPeers(peersPublicKeyMap: Map[InetSocketAddress, Option[ByteString]],
                      peersLastTimeUpdateMap: Map[InetSocketAddress, Long],
                      settings: Settings) {

  var lastHeightSyncTime: Map[InetSocketAddress, Long] = peersLastTimeUpdateMap

  def addOrUpdatePeer(peer: (InetSocketAddress, ByteString)): KnownPeers =
    if (!isSelfIp(peer._1)) this.copy(peersPublicKeyMap + (peer._1 -> Some(peer._2)))
    else this

  def updatePeerTime(peer: InetSocketAddress): KnownPeers =
    if (!isSelfIp(peer))
      this.copy(peersLastTimeUpdateMap = peersLastTimeUpdateMap + (peer -> System.currentTimeMillis()))
    else this

  def getPeersMessages(myAddr: InetSocketAddress, publicKey: ByteString): Seq[SendToNetwork] =
    peersPublicKeyMap.map(peer =>
        SendToNetwork(
          Peers(peersPublicKeyMap.flatMap {
            case (addr, Some(key)) => Some(addr -> key)
            case (_, None) => None
          }, (myAddr, publicKey), peer._1),
          peer._1
        )
    ).toSeq

  def getBlockMessage(block: KeyBlock): Seq[SendToNetwork] =
    peersPublicKeyMap.map(peer => SendToNetwork(Blocks(List(block)), peer._1)).toSeq

  def getTransactionMsg(transaction: Transaction): Seq[SendToNetwork] =
    peersPublicKeyMap.map(peer => SendToNetwork(Transactions(List(transaction)), peer._1)).toSeq

  def getHeightMessage(height: Long): Seq[SendToNetwork] = {
    val peersToSync = lastHeightSyncTime
      .filter(_._2 < System.currentTimeMillis() - settings.network.heightMessageInterval).keys
    println(peersLastTimeUpdateMap.mkString(","))
    println(s"After filter: ${peersToSync.mkString(",")}")
    lastHeightSyncTime ++= peersToSync.map(_ -> System.currentTimeMillis()).toMap
    peersToSync.map(peer => SendToNetwork(LastBlockHeight(height), peer)).toSeq
  }

  def isSelfIp(addr: InetSocketAddress): Boolean =
    (InetAddress.getLocalHost.getAddress sameElements addr.getAddress.getAddress) ||
      (InetAddress.getLoopbackAddress.getAddress sameElements addr.getAddress.getAddress)
}

object KnownPeers {

  def apply(settings: Settings): KnownPeers =
    new KnownPeers(
      settings.otherNodes.map(node => new InetSocketAddress(node.host, node.port) -> None).toMap,
      settings.otherNodes.map(node => (new InetSocketAddress(node.host, node.port), 0: Long)).toMap,
      settings
    )
}