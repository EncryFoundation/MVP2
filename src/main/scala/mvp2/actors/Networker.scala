package mvp2.actors

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.Props
import mvp2.actors.Networker.Peer
import mvp2.messages._
import mvp2.utils.Settings

class Networker(settings: Settings) extends CommonActor {

  var knownPeers: List[Peer] = settings.otherNodes.map(node =>
    Peer(new InetSocketAddress(node.host, node.port), System.currentTimeMillis())
  )

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(sendPeers)
    context.system.scheduler.schedule(1.seconds, 3.seconds){
      logger.info(s"KnownPeers: ${knownPeers.mkString(",")}")
    }
    if (settings.influx.isDefined && settings.testingSettings.exists(_.pingPong))
      context.system.scheduler.schedule(1.seconds, settings.heartbeat.seconds)(pingAllPeers)
    bornKids()
  }

  override def specialBehavior: Receive = {
    case msgFromRemote: MessageFromRemote =>
      updatePeerTime(msgFromRemote.remote)
      msgFromRemote.message match {
        case Peers(peers, remote) =>
          peers.foreach(addPeer)
        case Ping =>
          logger.info(s"Get ping from: ${msgFromRemote.remote} send Pong")
          context.actorSelection("/user/starter/networker/sender") ! SendToNetwork(Pong, msgFromRemote.remote)
        case Pong =>
          logger.info(s"Get pong from: ${msgFromRemote.remote} send Pong")
      }
  }

  def addPeer(peerAddr: InetSocketAddress): Unit = {
    logger.info(s"knownPeers.par.exists(_.remoteAddress != $peerAddr) = ${knownPeers.exists(_.remoteAddress != peerAddr)}")
    if (knownPeers.exists(_.remoteAddress != peerAddr)) {
      logger.info(s"Add new peer: $peerAddr to knownPeers")
      knownPeers = knownPeers :+ Peer(peerAddr, System.currentTimeMillis())
    }
  }

  def updatePeerTime(peer: InetSocketAddress): Unit =
    if (knownPeers.par.exists(_.remoteAddress == peer))
      knownPeers.find(_.remoteAddress == peer).foreach ( prevPeer =>
        knownPeers = knownPeers.filter(_ != prevPeer) :+ prevPeer.copy(lastMessageTime = System.currentTimeMillis())
      )

  def pingAllPeers: Unit =
    knownPeers.foreach(peer =>
      context.actorSelection("/user/starter/networker/sender") ! SendToNetwork(Ping, peer.remoteAddress)
    )

  def sendPeers: Unit =
    knownPeers.foreach(peer =>
      context.actorSelection("/user/starter/networker/sender") !
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
}

object Networker {

  case class Peer(remoteAddress: InetSocketAddress,
                  lastMessageTime: Long)

}

