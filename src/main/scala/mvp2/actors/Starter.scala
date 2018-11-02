package mvp2.actors

import akka.actor.{Actor, Props}
import mvp2.utils.Settings
import com.typesafe.scalalogging.StrictLogging
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

class Starter extends Actor with StrictLogging {

  import mvp2.messages.InfoMessage

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
    context.actorOf(Props(classOf[Blockchainer]), "blockchainer")
    settings.influx.foreach(influxSettings =>
      context.actorOf(Props(classOf[InfluxActor], influxSettings), name = "influxActor")
    )
    context.actorOf(Props[Informator])
    context.actorOf(Props[Zombie])
  }
}