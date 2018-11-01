package mvp2.actors

import akka.actor.ActorSelection
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import mvp2.data._
import mvp2.Messages.Get
import mvp2.utils.Settings
import scala.collection.immutable.HashMap

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging {

  var appendix: Appendix = Appendix(HashMap())
  val accountRef: ActorSelection = context.system.actorSelection("/user/starter/account")

  override def receiveRecover: Receive =
    if (settings.levelDBSettings.enableRestore) receiveRecoveryEnable else receiveRecoveryDisable

  def receiveRecoveryEnable: Receive = {
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case RecoveryCompleted =>
  }

  def receiveRecoveryDisable: Receive = {
    case _ => logger.info("Recovery is disabled.")
  }

  override def receiveCommand: Receive = {
    case block: Block => saveModifier(block)
    case Get => Blockchain.chain
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  def saveModifier(block: Block): Unit = {
    if (block.isValid) {
      block match {
        case keyBlock: KeyBlock =>
          logger.info(s"KeyBlock is valid with height ${keyBlock.height}.")
          val oldAppendix: HashMap[Int, Block] = appendix.chain
          oldAppendix.foreach(block =>
            persist(block) { x =>
              logger.info(s"Successfully saved block with id: ${x._2} and height ${x._1}!")
            })
          Blockchain.update(oldAppendix)
          appendix = appendix.copy(HashMap(keyBlock.height -> keyBlock))
          accountRef ! keyBlock
        case microBlock: MicroBlock =>
          logger.info(s"KeyBlock is valid with height ${microBlock.height}.")
          appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
          accountRef ! microBlock
      }
    }
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}