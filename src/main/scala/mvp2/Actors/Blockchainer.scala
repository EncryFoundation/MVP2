package mvp2.Actors

import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import mvp2.Data._
import scala.collection.immutable.HashMap

class Blockchainer extends PersistentActor with StrictLogging {

  var appendix: Appendix = Appendix(HashMap())

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case block: Block =>
      if (block.isValid) {
        block match {
          case keyBlock: KeyBlock =>
            val oldAppendix: HashMap[Int, Block] = appendix.chain
            oldAppendix.foreach(block => persist(block) { x =>
              logger.info(s"Successfully saved block with id: ${x._2} and height ${x._1}!")
            })
            Blockchain.upgrade(oldAppendix)
            appendix = appendix.copy(HashMap(keyBlock.height -> keyBlock))
          case microBlock: MicroBlock =>
            appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
        }
      }
    case _ => logger.info("Got something strange in Blockchainer!")
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}