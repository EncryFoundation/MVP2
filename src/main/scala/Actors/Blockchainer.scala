package Actors

import Data.{Block, Blockchain, KeyBlock, MicroBlock}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import scala.collection.immutable.HashMap

class Blockchainer extends PersistentActor with StrictLogging {

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case block: Block => updateChain(block)
    case _ => logger.info("Got something strange")
  }

  def updateChain(block: Block): Unit = {
    if (validate(block)) {
      block match {
        case keyBlock: KeyBlock =>
          val blockchainForWrite: HashMap[Int, Block] = Blockchain.getLastEpoch
          persist(blockchainForWrite) { x =>
            logger.info(s"Last epoch successfully saved! Size of map is: ${x.size}")
          }
          Blockchain.update(keyBlock)

        case microBlock: MicroBlock => Blockchain.update(microBlock)
      }
    }
  }

  def validate(block: Block): Boolean = block match {
    case keyBlock: KeyBlock => true
    case microBlock: MicroBlock => true
  }


  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}