package Actors

import akka.actor.{Actor, DeadLetter, UnhandledMessage}

class Zombie extends Actor {

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[DeadLetter])
    context.system.eventStream.subscribe(self, classOf[UnhandledMessage])
  }

  override def receive: Receive = {
    case deadMessage: DeadLetter => println(s"Dead letter: ${deadMessage.toString}.")
    case unhandled: UnhandledMessage => println(s"Unhandled message ${unhandled.toString}")
  }

}