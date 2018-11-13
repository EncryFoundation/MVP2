package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorSelection, Props}
import akka.util.ByteString
import mvp2.actors.Networker.Peer
import mvp2.data.KeyBlock
import mvp2.messages._
import mvp2.utils.{ECDSA, Settings}

class Networker(settings: Settings) extends CommonActor {

  var publicKey: Option[ByteString] = None

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  val keyKeeper: ActorSelection = context.actorSelection("/user/starter/blockchainer/planner/keyKeeper")

  val networkSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/sender")

  var peers: Map[Peer, Option[ByteString]] = settings.otherNodes.map(node =>
    Peer(new InetSocketAddress(node.host, node.port), System.currentTimeMillis()) -> None
  ).toMap

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(sendPeers())
    if (settings.influx.isDefined && settings.testingSettings.exists(_.pingPong))
      context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(pingAllPeers())
    bornKids()
  }

  override def specialBehavior: Receive = {
    case msgFromRemote: MessageFromRemote =>
      addOrUpdatePeer(msgFromRemote.remote -> None)
      updatePeerTime(msgFromRemote.remote)
      msgFromRemote.message match {
        case Peers(peersFromRemote, _) =>
          peersFromRemote.foreach(addOrUpdatePeer)
        case Ping =>
          logger.info(s"Get ping from: ${msgFromRemote.remote} send Pong")
          networkSender ! SendToNetwork(Pong, msgFromRemote.remote)
        case Pong =>
          logger.info(s"Get pong from: ${msgFromRemote.remote} send Pong")
        case Blocks(blocks) =>
          logger.info(s"Get blocks: ${blocks.mkString(",")}")
      }
    case myPublishedBlock: KeyBlock =>
      logger.info(s"Networker received published block with height: ${myPublishedBlock.height} to broadcast. " +
        s"But broadcasting yet implemented not.")
    case MyPublicKey(key) => publicKey = Some(ECDSA.compressPublicKey(key))
  }

  def addOrUpdatePeer(peer: (InetSocketAddress, Option[ByteString])): Unit =
    peers.find(_._1.remoteAddress == peer._1) match {
      case Some(peerInfo) => if (peerInfo._2.isEmpty && peer._2.isDefined) {
        peer._2.foreach(updatePeerKey)
        peers = (peers - peerInfo._1) + (Peer(peer._1, 0) -> peer._2)
      }
      case None =>
        peers = peers + (Peer(peer._1, 0) -> peer._2)
        peer._2.foreach(updatePeerKey)
    }

  def updatePeerKey(serializedKey: ByteString): Unit =
    keyKeeper ! PeerPublicKey(ECDSA.uncompressPublicKey(serializedKey))

  def updatePeerTime(peer: InetSocketAddress): Unit =
    if (peers.keys.toList.exists(_.remoteAddress == peer))
      peers.find(_._1.remoteAddress == peer).foreach (prevPeer =>
        peers = peers.filter(_ != prevPeer) +
          (prevPeer._1.copy(lastMessageTime = System.currentTimeMillis()) -> prevPeer._2)
      )

  def pingAllPeers(): Unit =
    peers.foreach(peer =>
      networkSender ! SendToNetwork(Ping, peer._1.remoteAddress)
    )

  def sendPeers(): Unit =
    peers.foreach(peer =>
      networkSender !
        SendToNetwork(
          Peers(peers.map(peer => (peer._1.remoteAddress, peer._2)), (myAddr, publicKey), peer._1.remoteAddress),
          peer._1.remoteAddress
        )
    )

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Receiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "receiver")
    context.actorOf(Props(classOf[Sender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "sender")
  }
}

object Networker {

  case class Peer(remoteAddress: InetSocketAddress,
                  lastMessageTime: Long)
}