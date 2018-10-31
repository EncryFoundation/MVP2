package mvp2.Actors

import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import mvp2.Data._
import mvp2.Messages.GetCurrentInfoFromBlockchainer
import mvp2.Utils.Settings
import scala.collection.immutable.HashMap

class Blockchainer extends PersistentActor with StrictLogging {

  var appendix: Appendix = Appendix(HashMap())

  override def receiveRecover: Receive =
    if (Settings.settings.levelDBSettings.enableRestore) receiveRecoveryEnable else receiveRecoveryDisable

  def receiveRecoveryEnable: Receive = {
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case RecoveryCompleted =>
  }

  def receiveRecoveryDisable: Receive = {
    case _ => logger.info("Recovery disabled")
  }

  override def receiveCommand: Receive = {
    case block: Block => saveModifier(block)
    case GetCurrentInfoFromBlockchainer => Blockchain.chain
    case _ => logger.info("Got something strange in Blockchainer!")
  }

  def saveModifier(block: Block): Unit = {
    if (block.isValid) {
      block match {
        case keyBlock: KeyBlock =>
          logger.info(s"KeyBlock is valid with height ${keyBlock.height}")
          val oldAppendix: HashMap[Int, Block] = appendix.chain
          oldAppendix.foreach(block =>
            persist(block) { x =>
              logger.info(s"Successfully saved block with id: ${x._2} and height ${x._1}!")
            })
          Blockchain.upgrade(oldAppendix)
          appendix = appendix.copy(HashMap(keyBlock.height -> keyBlock))
          context.system.actorSelection("/user/starter/account") ! keyBlock
        case microBlock: MicroBlock =>
          logger.info(s"KeyBlock is valid with height ${microBlock.height}")
          appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
          context.system.actorSelection("/user/starter/account") ! microBlock
      }
    }
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}