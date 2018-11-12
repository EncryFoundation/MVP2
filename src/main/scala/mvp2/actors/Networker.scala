package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.Props
import mvp2.actors.Networker.Peer
import mvp2.data.KeyBlock
import mvp2.messages._
import mvp2.utils.Settings

class Networker(settings: Settings) extends CommonActor {

  var knownPeers: List[Peer] = settings.otherNodes.map(node =>
    Peer(new InetSocketAddress(node.host, node.port), System.currentTimeMillis())
  )

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(sendPeers())
    if (settings.influx.isDefined && settings.testingSettings.exists(_.pingPong))
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
          context.actorSelection("/user/starter/blockchainer/networker/sender") !
            SendToNetwork(Pong, msgFromRemote.remote)
        case Pong =>
          logger.info(s"Get pong from: ${msgFromRemote.remote} send Pong")
        case Blocks(blocks) =>
          logger.info(s"Receive blocks: ${blocks.mkString(",")} from remote: ${msgFromRemote.remote}")
        case SyncMessageIterators(iterators) =>
          if (settings.testingSettings.exists(_.messagesTime)) {
            context.actorSelection("/user/starter/influxActor") !
              SyncMessageIteratorsFromRemote(iterators, msgFromRemote.remote)
          }
      }
    case keyBlock: KeyBlock =>
      knownPeers.foreach(peer =>
        context.actorSelection("/user/starter/blockchainer/networker/sender") !
          SendToNetwork(Blocks(List(keyBlock)), peer.remoteAddress)
      )
  }

  def addPeer(peerAddr: InetSocketAddress): Unit =
    if (!knownPeers.map(_.remoteAddress).contains(peerAddr) && !isSelfIp(peerAddr))
      knownPeers = knownPeers :+ Peer(peerAddr, 0)

  def updatePeerTime(peer: InetSocketAddress): Unit =
    if (knownPeers.par.exists(_.remoteAddress == peer))
      knownPeers.find(_.remoteAddress == peer).foreach ( prevPeer =>
        knownPeers = knownPeers.filter(_ != prevPeer) :+ prevPeer.copy(lastMessageTime = System.currentTimeMillis())
      )

  def pingAllPeers(): Unit =
    knownPeers.foreach(peer =>
      context.actorSelection("/user/starter/blockchainer/networker/sender") ! SendToNetwork(Ping, peer.remoteAddress)
    )

  def sendPeers(): Unit =
    knownPeers.foreach(peer =>
      context.actorSelection("/user/starter/blockchainer/networker/sender") !
        SendToNetwork(
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