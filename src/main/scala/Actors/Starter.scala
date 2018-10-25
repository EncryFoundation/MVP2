package Actors

import akka.actor.{Actor, Props}
import utils.Settings
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Starter extends Actor {

  import Messages.InfoMessage

  context.system.scheduler
    .schedule(0 seconds, 3 seconds, self, InfoMessage("Self ping by sheduler."))

  val settings: Settings = Settings.settings

  override def preStart(): Unit = {
    println("Starting the Starter!")
    bornKids()
  }

  override def receive: Receive = {
    case message: InfoMessage => println(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "networker")
    //context.actorOf(Props[InfluxActor], "influxActor")
    context.actorOf(Props[Informator])
    context.actorOf(Props[Zombie])
  }

}
