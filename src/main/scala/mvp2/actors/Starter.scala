package mvp2.actors

import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import akka.actor.Props
import mvp2.utils.{DbService, Settings}

class Starter extends CommonActor {

  import mvp2.messages.InfoMessage

  val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load)
    .as[Settings]("mvp")

  override def preStart(): Unit = {
    logger.info("Starting the Starter!")
    bornKids()
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => logger.info(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[Blockchainer], settings), "blockchainer")
    settings.influx.foreach(influxSettings =>
      context.actorOf(Props(classOf[InfluxActor], influxSettings), name = "influxActor")
    )
    context.actorOf(Props(classOf[ConsoleActor], settings), "cliActor")
    context.actorOf(Props(classOf[Informator], settings), "informator")
    context.actorOf(Props(classOf[Zombie]), "zombie")
    context.actorOf(Props(classOf[ConsoleActor], settings), "cliActor")
    settings.postgres.foreach { pgSettings =>
      if (pgSettings.read || pgSettings.write) {
        val dbService = new DbService(pgSettings)
        if (pgSettings.write) context.actorOf(Props(classOf[PgWriter], dbService), "pgWriter")
        if (pgSettings.read)
          context.actorOf(Props(classOf[PgReader], dbService, settings.postgres.flatMap(_.batchSize).getOrElse(5)), "pgReader")
      }
    }
  }
}