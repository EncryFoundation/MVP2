package mvp2.actors

import akka.actor.{Actor, Props}
import mvp2.utils.Settings
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import mvp2.Actors.Zombie
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

class Starter extends Actor with StrictLogging {

  import mvp2.Messages.InfoMessage

  context.system.scheduler
    .schedule(0 seconds, 3 seconds, self, InfoMessage("Self ping by sheduler."))

  val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load)
    .as[Settings]("mvp")

  override def preStart(): Unit = {
    logger.info("Starting the Starter!")
    bornKids()
  }

  override def receive: Receive = {
    case message: InfoMessage => logger.info(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "networker")
    context.actorOf(Props(classOf[Blockchainer], settings), "blockchainer")
    //context.actorOf(Props[Informator], settings, "informator")
    context.actorOf(Props[Zombie])
  }

}