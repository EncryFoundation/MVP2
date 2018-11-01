package mvp2.actors

import akka.actor.{ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data._
import mvp2.messages.Get
import scala.collection.immutable.HashMap

class Blockchainer extends PersistentActor with StrictLogging {

  var appendix: Appendix = Appendix(HashMap())
  val accountant: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/accountant")

  context.actorOf(Props(classOf[Accountant]), "accountant")

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => Blockchain.update(keyBlock)
    case microBlock: MicroBlock => Blockchain.update(microBlock)
    case RecoveryCompleted =>
      context.actorOf(Props[Publisher], "publisher")
      val lastBlock: Block = Blockchain.chain.toSeq.sortBy(_._1).lastOption.map(_._2).getOrElse(
        KeyBlock(0, System.currentTimeMillis(), ByteString.empty, List())
      )
      context.actorSelection("/user/starter/blockchainer/publisher") ! lastBlock
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
          appendix.chain.foreach(block =>
            persist(block._2) { x =>
              logger.info(s"Successfully saved block with id: ${x.currentBlockHash} and height ${x.height}!")
            })
          Blockchain.update(appendix.chain)
          appendix = appendix.copy(HashMap(keyBlock.height -> keyBlock))
          accountant ! keyBlock
        case microBlock: MicroBlock =>
          logger.info(s"KeyBlock is valid with height ${microBlock.height}.")
          appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
          accountant ! microBlock
      }
    }
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}