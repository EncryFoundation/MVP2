package mvp2.Actors

import akka.actor.{Actor, Props}
import mvp2.Utils.Settings
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Starter extends Actor with StrictLogging {

  import mvp2.Messages.InfoMessage

  context.system.scheduler
    .schedule(0 seconds, 3 seconds, self, InfoMessage("Self ping by sheduler."))

  val settings: Settings = Settings.settings

  override def preStart(): Unit = {
    logger.info("Starting the Starter!")
    bornKids()
  }

  override def receive: Receive = {
    case message: InfoMessage => logger.info(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Networker].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "networker")
    //context.actorOf(Props[InfluxActor], "influxActor")
    context.actorOf(Props[Informator], "informator")
    context.actorOf(Props[Zombie])
  }

}
