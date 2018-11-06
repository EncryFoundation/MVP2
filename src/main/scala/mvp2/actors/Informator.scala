package mvp2.actors

import akka.actor.ActorRefFactory
import mvp2.messages.Get
import mvp2.http.ApiRoute
import mvp2.messages.CurrentBlockchainInfo
import mvp2.utils.Settings
import akka.http.scaladsl.Http
import mvp2.MVP2._
import mvp2.utils.Settings

class Informator(settings: Settings) extends CommonActor {

  Informator.start(settings, context)

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo()

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo => actualInfo = currentBlockchainInfo
  }
}

object Informator {

  def start(settings: Settings, context: ActorRefFactory): Unit = Http().bindAndHandle(
    ApiRoute(settings, context).apiInfo, settings.apiSettings.httpHost, settings.apiSettings.httpPort
  )
}