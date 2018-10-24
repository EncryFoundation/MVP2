package Actors

import akka.actor.{Actor, Props}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class Starter extends Actor {

  import Messages.{Start, InfoMessage}

  context.system.scheduler.schedule(0 seconds, 3 seconds, self, InfoMessage("Self ping by sheduler."))

  override def preStart(): Unit = {
    println("Starting the Starter!")
  }

  override def receive: Receive = {
    case Start => bornKids()
    case message: InfoMessage => println(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Networker].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "networker")
    context.actorOf(Props[InfluxActor], "influx")
  }

}
