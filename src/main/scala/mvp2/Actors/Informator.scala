package mvp2.Actors

import mvp2.Messages.Get
import mvp2.Messages.CurrentBlockchainInfo

class Informator extends CommonActor {

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo => actualInfo = currentBlockchainInfo
  }
}