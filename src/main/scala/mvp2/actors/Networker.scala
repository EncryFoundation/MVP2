package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{ActorSelection, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.util.ByteString
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages.{Blocks, Peers, SyncMessageIterators, Transactions}
import mvp2.data.{KeyBlock, KnownPeers, Transaction}
import mvp2.utils.Settings

class Networker(settings: Settings) extends CommonActor {

  var myPublicKey: Option[ByteString] = None

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  val keyKeeper: ActorSelection = context.actorSelection("/user/starter/blockchainer/planner/keyKeeper")

  val udpSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/udpSender")

  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

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
              updatePeerKey(peerToAddOrUpdate._2)
              newKnownPeers.addOrUpdatePeer(peerToAddOrUpdate._1, peerToAddOrUpdate._2)
                .updatePeerTime(msgFromNetwork.remote)
          }
        case Blocks(_) =>
        case SyncMessageIterators(iterators) =>
          context.actorSelection("/user/starter/influxActor") !
            SyncMessageIteratorsFromRemote(iterators, msgFromNetwork.remote)
        case Transactions(transactions) =>
          logger.info(s"Got ${transactions.size} new transactions.")
          transactions.foreach(tx => publisher ! tx)
      }
    case MyPublicKey(key) => myPublicKey = Some(key)
    case keyBlock: KeyBlock => peers.getBlockMsg(keyBlock).foreach(msg => udpSender ! msg)
    case transaction: Transaction => peers.getTransactionMsg(transaction).foreach(msg => udpSender ! msg)
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
    val a = context.actorOf(Props(classOf[UdpReceiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "udpReceiver")
    //println(s"The path is: ${a.path}")
    val b = context.actorOf(Props(classOf[UdpSender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "udpSender")
    //println(s"The path is: ${b.path} && ${b}")

  }
}