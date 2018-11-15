package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorSelection, Props}
import mvp2.actors.Networker.Peer
import mvp2.data.{KeyBlock, Transaction}
import mvp2.messages._
import mvp2.utils.Settings

class Networker(settings: Settings) extends CommonActor {

  var knownPeers: List[Peer] = settings.otherNodes.map(node =>
    Peer(new InetSocketAddress(node.host, node.port), System.currentTimeMillis())
  )
  val senderActor: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/sender")
  val publisher: ActorSelection = context.actorSelection("/user/starter/blockchainer/publisher")

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(sendPeers())
    if (settings.testingSettings.pingPong)
      context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(pingAllPeers())
    bornKids()
  }

  override def specialBehavior: Receive = {
    case msgFromRemote: MessageFromRemote =>
      updatePeerTime(msgFromRemote.remote)
      msgFromRemote.message match {
        case Peers(peers, _) =>
          (peers :+ msgFromRemote.remote).foreach(addPeer)
        case Ping =>
          logger.info(s"Get ping from: ${msgFromRemote.remote} send Pong")
          senderActor ! SendToNetwork(Pong, msgFromRemote.remote)
        case Pong =>
          logger.info(s"Get pong from: ${msgFromRemote.remote} send Pong")
        case Blocks(blocks) =>
          logger.info(s"Receive blocks: ${blocks.mkString(",")} from remote: ${msgFromRemote.remote}")
        case SyncMessageIterators(iterators) =>
          context.actorSelection("/user/starter/influxActor") !
            SyncMessageIteratorsFromRemote(iterators, msgFromRemote.remote)
        case Transactions(transactions) =>
          transactions.foreach { tx =>
            println(s"Got new transaction $tx from remote ${msgFromRemote.remote}")
            publisher ! tx
          }
      }
    case keyBlock: KeyBlock =>
      knownPeers.foreach(peer =>
        senderActor ! SendToNetwork(Blocks(List(keyBlock)), peer.remoteAddress)
      )
    case transaction: Transaction => knownPeers.foreach(peer =>
      senderActor ! SendToNetwork(Transactions(List(transaction)), peer.remoteAddress)
    )
  }

  def addPeer(peerAddr: InetSocketAddress): Unit =
    if (!knownPeers.map(_.remoteAddress).contains(peerAddr) && !isSelfIp(peerAddr))
      knownPeers = knownPeers :+ Peer(peerAddr, 0)

  def updatePeerTime(peer: InetSocketAddress): Unit =
    if (knownPeers.par.exists(_.remoteAddress == peer))
      knownPeers.find(_.remoteAddress == peer).foreach(prevPeer =>
        knownPeers = knownPeers.filter(_ != prevPeer) :+ prevPeer.copy(lastMessageTime = System.currentTimeMillis())
      )

  def pingAllPeers(): Unit =
    knownPeers.foreach(peer => senderActor ! SendToNetwork(Ping, peer.remoteAddress))

  def sendPeers(): Unit =
    knownPeers.foreach(peer =>
      senderActor ! SendToNetwork(
        Peers(
          knownPeers.par.filter(_.remoteAddress != peer.remoteAddress).toList.map(_.remoteAddress),
          peer.remoteAddress
        ),
        peer.remoteAddress
      )
    )

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Receiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "receiver")
    context.actorOf(Props(classOf[Sender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "sender")
  }

  def isSelfIp(addr: InetSocketAddress): Boolean =
    (InetAddress.getLocalHost.getAddress sameElements addr.getAddress.getAddress) ||
      (InetAddress.getLoopbackAddress.getAddress sameElements addr.getAddress.getAddress)
}

object Networker {

  case class Peer(remoteAddress: InetSocketAddress,
                  lastMessageTime: Long)

}