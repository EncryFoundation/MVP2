package Actors

import java.net.InetSocketAddress

import Data.BlockchainCompareStatus.Unknown
import Data.Peer
import Messages.{KnownPeers, MessageFromRemote}
import akka.actor.Props

class Networker extends CommonActor {

  import Messages.InfoMessage

  var knownPeers: List[Peer] = List.empty

  override def preStart(): Unit = {
    println("Starting the Networker!")
    bornKids()
  }

  def addOrUpdatePeer(peerAddr: InetSocketAddress): Unit = {
    if (knownPeers.par.exists(_.remoteAddress == peerAddr)) {
      knownPeers.par.find(_.remoteAddress == peerAddr).foreach(prevPeer =>
          knownPeers = knownPeers.filter(_ == prevPeer) :+ prevPeer.copy(lastPollingTime = System.currentTimeMillis())
      )
    } else
      knownPeers = knownPeers :+ Peer(peerAddr, Unknown, System.currentTimeMillis())
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => println(message.info)
    case msgFromRemote: MessageFromRemote =>
      addOrUpdatePeer(msgFromRemote.remote)
      msgFromRemote.message match {
        case KnownPeers(peers) => peers.foreach(addOrUpdatePeer)
        case _ => //Another messages
      }
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Sender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
    context.actorOf(Props[Receiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
  }
}

