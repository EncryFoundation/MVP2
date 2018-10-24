package Actors

import Messages.Get
import akka.actor.Actor

class Informator extends Actor {

  import Messages.CurrentBlockchainInfo

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)

  override def preStart(): Unit = {
    println("Starting the Informator!")
  }

  override def receive: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo =>
    case smth: Any => println(s"Got smth strange: $smth.")
  }

}