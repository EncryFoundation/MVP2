package Actors

import java.net.InetSocketAddress

import Data.Peer
import Messages.{BroadcastPeers, KnownPeers, MessageFromRemote}
import akka.actor.Props

class Networker extends CommonActor {

  import Messages.InfoMessage

  val testPeer: Peer = Peer(
    new InetSocketAddress("localhost", 5678),
    0L
  )

  var knownPeers: List[Peer] = List(
    testPeer
  )

  override def preStart(): Unit = {
    println("Starting the Networker!")
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
        case KnownPeers(peers) => peers.foreach(addOrUpdatePeer)
        case _ => //Another messages
      }
    case BroadcastPeers =>
      knownPeers.foreach(peer =>
        context.actorSelection("/user/starter/networker/sender") !
          KnownPeers(knownPeers.par.filter(_.remoteAddress != peer.remoteAddress).toList.map(_.remoteAddress))
      )
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Sender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
    context.actorOf(Props[Receiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
  }
}

