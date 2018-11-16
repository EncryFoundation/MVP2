package mvp2.actors

import akka.actor.{ActorRef, Props}
import mvp2.utils.Settings
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

class Starter extends CommonActor {

  val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load)
    .as[Settings]("mvp")

  override def preStart(): Unit = {
    logger.info("Starting the Starter!")
    bornKids()
  }

  override def specialBehavior: Receive = {
    case message: String => logger.info(message)
  }

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Blockchainer], settings), "blockchainer")
    context.actorOf(Props(classOf[InfluxActor], settings), name = "influxActor")
    context.actorOf(Props(classOf[ConsoleActor], settings), "cliActor")
    context.actorOf(Props(classOf[Zombie]), "zombie")
    context.actorOf(Props(classOf[Informator], settings), "informator")
    context.actorOf(Props(classOf[TimeProvider], settings), "timeProvider")
  }
}