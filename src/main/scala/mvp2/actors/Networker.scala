package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{ActorSelection, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.util.ByteString
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages._
import mvp2.data.{KeyBlock, KnownPeers, Transaction}
import mvp2.utils.Settings

class Networker(settings: Settings) extends CommonActor {

  var myPublicKey: Option[ByteString] = None

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  val keyKeeper: ActorSelection = context.actorSelection("/user/starter/blockchainer/planner/keyKeeper")

  val udpSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/udpSender")

  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  val influxActor: ActorSelection = context.actorSelection("/user/starter/influxActor")

  val planner: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/planner")

  var peers: KnownPeers = KnownPeers(settings)

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.millisecond)(sendPeers())
    bornKids()
  }

  override def specialBehavior: Receive = {
    case msgFromNetwork: MsgFromNetwork =>
      msgFromNetwork.message match {
        case Peers(peersFromRemote, _) =>
          peers = peersFromRemote.foldLeft(peers) {
            case (newKnownPeers, peerToAddOrUpdate) =>
              updatePeerKey(peerToAddOrUpdate.publicKey)
              newKnownPeers.addOrUpdatePeer(peerToAddOrUpdate.addr, peerToAddOrUpdate.publicKey)
                .updatePeerTime(msgFromNetwork.remote)
          }
        case Blocks(blocks) =>
          if (blocks.nonEmpty) context.parent ! msgFromNetwork.message
        case SyncMessageIterators(iterators) =>
          influxActor ! SyncMessageIteratorsFromRemote(
            iterators.map(iterInfo => iterInfo.msgName -> iterInfo.msgIter).toMap,
            msgFromNetwork.remote)
        case LastBlockHeight(height) => context.parent ! CheckRemoteBlockchain(height, msgFromNetwork.remote)
        case Transactions(transactions) =>
          logger.info(s"Got ${transactions.size} new transactions.")
          transactions.foreach(tx => publisher ! tx)
      }
    case OwnBlockchainHeight(height) => peers.getHeightMessage(height).foreach(udpSender ! _)
    case MyPublicKey(key) => myPublicKey = Some(key)
    case transaction: Transaction => peers.getTransactionMsg(transaction).foreach(msg => udpSender ! msg)
    case keyBlock: KeyBlock => peers.getBlockMessage(keyBlock).foreach(udpSender ! _)
    case RemoteBlockchainMissingPart(blocks, remote) =>
      udpSender ! SendToNetwork(Blocks(blocks), remote)
  }

  def updatePeerKey(serializedKey: ByteString): Unit =
    if (!myPublicKey.contains(serializedKey)) {
      val newPublicKey: PeerPublicKey = PeerPublicKey(serializedKey)
      keyKeeper ! newPublicKey
      planner ! newPublicKey
    }

  def sendPeers(): Unit =
    myPublicKey.foreach(key => peers.getPeersMessages(myAddr, key).foreach(msg => udpSender ! msg))

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[UdpReceiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "udpReceiver")
    context.actorOf(Props(classOf[UdpSender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "udpSender")
  }
}