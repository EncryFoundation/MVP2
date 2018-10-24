package Actors

import Messages.Get

class Informator extends CommonActor {

  import Messages.CurrentBlockchainInfo

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)

  override def preStart(): Unit = {
    println("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo =>
  }

}