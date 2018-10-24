package Actors

import akka.actor.Actor

class Networker extends Actor {

  import Messages.InfoMessage

  override def preStart(): Unit = {
    println("Starting the Networker!")
    bornKids()
  }

  override def receive: Receive = {
    case message: InfoMessage => println(message.info)
    case smth: Any => println(s"Got smth strange: $smth.")
  }

  def bornKids(): Unit = {
    //context.actorOf(Props[UDPActor].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "udp")
    //context.actorOf(Props[UdpReceiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
    //context.actorOf(Props[UdpSender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
  }
}

