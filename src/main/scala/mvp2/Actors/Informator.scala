package mvp2.Actors

import mvp2.Messages.Get
import mvp2.http.ApiRoute
import mvp2.Messages.CurrentBlockchainInfo
import akka.http.scaladsl.Http
import mvp2.MVP2._
import mvp2.Utils.Settings

class Informator extends CommonActor {

  Informator.start()

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo => actualInfo = currentBlockchainInfo
  }
}

object Informator {

  def start(): Unit = Http().bindAndHandle(
    ApiRoute().apiInfo, Settings.settings.apiSettings.httpHost, Settings.settings.apiSettings.httpPort
  )
}