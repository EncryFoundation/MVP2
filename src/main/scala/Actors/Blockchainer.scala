package Actors

import Data.{Blockchain, KeyBlock, MicroBlock}
import akka.persistence.{PersistentActor, RecoveryCompleted}

class Blockchainer extends PersistentActor {

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case keyBlock: KeyBlock =>
      Blockchain.update(keyBlock)
    case microBlock: MicroBlock =>
      Blockchain.update(microBlock)
  }

  def updateChain(microBlock: MicroBlock): Unit = {

  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}


