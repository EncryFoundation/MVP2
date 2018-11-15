package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorSelection, Props}
import akka.util.ByteString
import mvp2.data.{KeyBlock, KnownPeers}
import mvp2.messages._
import mvp2.utils.{ECDSA, Settings}

class Networker(settings: Settings) extends CommonActor {

  var publicKey: Option[ByteString] = None

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  val keyKeeper: ActorSelection = context.actorSelection("/user/starter/blockchainer/planner/keyKeeper")

  val networkSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/sender")

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
          peers = peersFromRemote.foldLeft(peers){
            case (newKnownPeers, peerToAddOrUpdate) =>
              peerToAddOrUpdate._2.foreach(updatePeerKey)
              newKnownPeers.addOrUpdatePeer(peerToAddOrUpdate._1, peerToAddOrUpdate._2)
                .updatePeerTime(msgFromRemote.remote)
          }
        case Blocks(blocks) =>
          logger.info(s"Get blocks: ${blocks.mkString(",")}")
        case SyncMessageIterators(iterators) =>
          context.actorSelection("/user/starter/influxActor") !
            SyncMessageIteratorsFromRemote(iterators, msgFromRemote.remote)
      }
    case MyPublicKey(key) => publicKey = Some(ECDSA.compressPublicKey(key))
    case keyBlock: KeyBlock =>
      peers.getBlockMsg(keyBlock).foreach(msg =>
        context.actorSelection("/user/starter/blockchainer/networker/sender") ! msg
      )
  }

  def updatePeerKey(serializedKey: ByteString): Unit =
    keyKeeper ! PeerPublicKey(ECDSA.uncompressPublicKey(serializedKey))

  def sendPeers(): Unit = peers.getPeersMessages(myAddr, publicKey).foreach(msg => networkSender ! msg)

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Receiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "receiver")
    context.actorOf(Props(classOf[Sender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "sender")
  }
}