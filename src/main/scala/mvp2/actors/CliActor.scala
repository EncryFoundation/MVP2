package mvp2.actors

import akka.actor.{Actor, ActorSelection}
import mvp2.messages.{CurrentBlockchainInfo, Get}
import akka.pattern.ask
import akka.util.Timeout
import mvp2.utils.Settings
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class CliActor(settings: Settings) extends Actor {

  val promt: String = "$> "
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)

  override def preStart(): Unit = {
    scala.io.Source.stdin.getLines().foreach {
      print(promt)
      cliCommands
    }
  }

  val cliCommands: PartialFunction[String, Unit] = {
    case "node shutdown" => System.exit(0)
    case "help" => println(
       """node shutdown   -   Turn off the application.
         |help            -   Show info about commands.
         |info            -   Get current info about blockchain.
       """.stripMargin
    )
      print(promt)
    case "info" =>
      val currentInfo: Future[CurrentBlockchainInfo] = (informator ? Get).mapTo[CurrentBlockchainInfo]
      currentInfo.map(info => print(s"Height: ${info.height}. LastKeyBlock: ${info.lastKeyBlock}. " +
        s"LastMicroBlock: ${info.lastMicroBlock}.\n$promt"))
    case _ => print(s"Wrong command!\n$promt")
  }

  override def receive: Receive = {
    case _ =>
  }
}