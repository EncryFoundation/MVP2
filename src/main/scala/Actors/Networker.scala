package Actors

import java.net.InetSocketAddress
import Actors.Networker.Peer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import Messages._
import akka.actor.Props
import com.typesafe.scalalogging.StrictLogging
import utils.Settings

class Networker(settings: Settings) extends CommonActor with StrictLogging {

  import Messages.InfoMessage

  var knownPeers: List[Peer] = settings.otherNodes.map(node =>
    Peer(new InetSocketAddress(node.host, node.port), System.currentTimeMillis())
  )

  override def preStart(): Unit = {
    println("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, 1.seconds)(self ! BroadcastPeers)
    bornKids()
  }

  def addOrUpdatePeer(peerAddr: InetSocketAddress): Unit = {
    if (knownPeers.par.exists(_.remoteAddress == peerAddr)) {
      knownPeers.par.find(_.remoteAddress == peerAddr).foreach(prevPeer =>
          knownPeers = knownPeers.filter(_ == prevPeer) :+ prevPeer.copy(lastMessageTime = System.currentTimeMillis())
      )
    } else
      knownPeers = knownPeers :+ Peer(peerAddr, System.currentTimeMillis())
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => println(message.info)
    case msgFromRemote: MessageFromRemote =>
      addOrUpdatePeer(msgFromRemote.remote)
      msgFromRemote.message match {
        case KnownPeers(peers, remote) =>
          logger.info(s"Get known peers: $peers")
          peers.foreach(addOrUpdatePeer)
        case Ping =>
          logger.info(s"Get ping from: ${msgFromRemote.remote} send Pong")
          context.actorSelection("/user/starter/networker/sender") ! Pong
        case Pong =>
          logger.info(s"Get pong from: ${msgFromRemote.remote} send Pong")
        case _ => //Another messages
      }
    case BroadcastPeers =>
      knownPeers.foreach(peer =>
        context.actorSelection("/user/starter/networker/sender") !
          SendToNetwork(
            KnownPeers(
              knownPeers.par.filter(_.remoteAddress != peer.remoteAddress).toList.map(_.remoteAddress),
              peer.remoteAddress
            ),
            peer.remoteAddress
          )
      )
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Sender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
    context.actorOf(Props(new Receiver(settings)).withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
  }
}

object Networker {

  case class Peer(remoteAddress: InetSocketAddress,
                  lastMessageTime: Long)
}

