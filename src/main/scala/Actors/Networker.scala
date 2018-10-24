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
    context.actorOf(Props[Sender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
    context.actorOf(Props[Receiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
  }
}

