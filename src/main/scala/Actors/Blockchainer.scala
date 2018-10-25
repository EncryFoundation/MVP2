package Actors

import Data.{Blockchain, KeyBlock, MicroBlock}
import akka.persistence.{PersistentActor, RecoveryCompleted}

class Blockchainer extends PersistentActor {

  override def receiveRecover: Receive = {
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case microBlock: MicroBlock => updateHeaders(header)
    case microBlock: KeyBlock => updateHeaders(header)
  }

  def updateChain(microBlock: MicroBlock): Unit = {

  }
}


