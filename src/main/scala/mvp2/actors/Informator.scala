package mvp2.actors

import mvp2.messages.Get
import mvp2.messages.CurrentBlockchainInfo
import mvp2.utils.Settings

class Informator(settings: Settings) extends CommonActor {

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo => actualInfo = currentBlockchainInfo
  }
}