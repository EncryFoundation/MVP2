package Actors

import akka.actor.Props

class Networker extends CommonActor {

  import Messages.InfoMessage

  override def preStart(): Unit = {
    println("Starting the Networker!")
    bornKids()
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => println(message.info)
  }

  def bornKids(): Unit = {
    //context.actorOf(Props[UDPActor].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "udp")
    context.actorOf(Props[Sender], "sender")
    context.actorOf(Props[Receiver], "receiver")
  }
}

