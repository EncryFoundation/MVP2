package Actors

import java.net.InetSocketAddress

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import Data.Peer
import Messages._
import akka.actor.Props
import com.typesafe.scalalogging.StrictLogging

class Networker extends CommonActor with StrictLogging {

  import Messages.InfoMessage

  val testPeer1: Peer = Peer(
    new InetSocketAddress("127.0.0.1", 5678),
    0L
  )

  val testPeer2: Peer = Peer(
    new InetSocketAddress("127.0.0.1", 999),
    0L
  )

  var knownPeers: List[Peer] = List(
    testPeer1,
    testPeer2
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
      knownPeers.take(1).foreach(peer =>
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
    context.actorOf(Props[Receiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
  }
}

