package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.util.ByteString
import akka.actor.{ActorSelection, Props}
import mvp2.data.{KeyBlock, Transaction, KnownPeers}
import mvp2.messages._
import mvp2.utils.{ECDSA, Settings}

class Networker(settings: Settings) extends CommonActor {

  var myPublicKey: Option[ByteString] = None

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  val keyKeeper: ActorSelection = context.actorSelection("/user/starter/blockchainer/planner/keyKeeper")

  val networkSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/sender")

  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  var peers: KnownPeers = KnownPeers(settings)

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(sendPeers())
    bornKids()
  }

  override def specialBehavior: Receive = {
    case msgFromRemote: MessageFromRemote =>
      msgFromRemote.message match {
        case Peers(peersFromRemote, _) =>
          peers = peersFromRemote.foldLeft(peers) {
            case (newKnownPeers, peerToAddOrUpdate) =>
              updatePeerKey(peerToAddOrUpdate._2)
              newKnownPeers.addOrUpdatePeer(peerToAddOrUpdate._1, peerToAddOrUpdate._2)
                .updatePeerTime(msgFromRemote.remote)
          }
        case Blocks(_) =>
        case SyncMessageIterators(iterators) =>
          context.actorSelection("/user/starter/influxActor") !
            SyncMessageIteratorsFromRemote(iterators, msgFromRemote.remote)
        case Transactions(transactions) =>
          transactions.foreach { tx =>
            logger.info(s"Got new transaction $tx from remote ${msgFromRemote.remote}")
            publisher ! tx
          }
      }
    case MyPublicKey(key) => myPublicKey = Some(ECDSA.compressPublicKey(key))
    case keyBlock: KeyBlock => peers.getBlockMsg(keyBlock).foreach(msg => networkSender ! msg)
    case transaction: Transaction => peers.getTransactionMsg(transaction).foreach(msg => networkSender ! msg)
  }

  def updatePeerKey(serializedKey: ByteString): Unit =
    if (!myPublicKey.contains(serializedKey)) keyKeeper ! PeerPublicKey(ECDSA.uncompressPublicKey(serializedKey))

  def sendPeers(): Unit =
    myPublicKey.foreach(key => peers.getPeersMessages(myAddr, key).foreach(msg => networkSender ! msg))

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Receiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "receiver")
    context.actorOf(Props(classOf[Sender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "sender")
  }
}