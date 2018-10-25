package Actors

import Messages.Get
import http.HttpServer
import Messages.CurrentBlockchainInfo

class Informator extends CommonActor {

  HttpServer.start()
  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo => actualInfo = currentBlockchainInfo
  }
}