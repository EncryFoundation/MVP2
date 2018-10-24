package Actors

import akka.actor.{Actor, Props}

class Starter extends Actor {

  import Messages.{Start, InfoMessage}

  override def preStart(): Unit = {
    println("Starting the Starter!")
  }

  override def receive: Receive = {
    case Start => bornKids()
    case message: InfoMessage => println(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Networker].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "networker")
  }

}
