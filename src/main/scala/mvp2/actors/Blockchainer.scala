package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data._
import mvp2.utils.Settings
import mvp2.messages.{CurrentBlockchainInfo, Get}
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Blockchainer(settings: Settings) extends PersistentActor with Blockchain with StrictLogging {

  var appendix: Appendix = Appendix(TreeMap())
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")

  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant]), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props[Publisher], "publisher")

  context.system.scheduler.schedule(1.seconds, 1.seconds) {
    informator ! CurrentBlockchainInfo(
      appendix.chain.lastOption.map(_._1).getOrElse(0),
      None,
      None
    )
  }

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => update(keyBlock)
    case microBlock: MicroBlock => update(microBlock)
    case RecoveryCompleted =>
      publisher ! lastKeyBlock.getOrElse(
        KeyBlock(0, System.currentTimeMillis(), ByteString.empty, List())
      )
  }

  override def receiveCommand: Receive = {
    case block: Block => saveModifier(block)
    case Get => chain
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  def saveModifier(block: Block): Unit = if (block.isValid) block match {
    case keyBlock: KeyBlock =>
      logger.info(s"New keyBlock with height ${keyBlock.height} is received on blockchainer.")
      appendix.chain.foreach(block =>
        persist(block._2) { x =>
          logger.info(s"Successfully saved block with id: ${x.currentBlockHash} and height ${x.height}!")
        })
      update(appendix.chain)
      appendix = appendix.copy(TreeMap(keyBlock.height -> keyBlock))
      accountant ! keyBlock
    case microBlock: MicroBlock =>
      logger.info(s"KeyBlock is valid with height ${microBlock.height}.")
      appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
      accountant ! microBlock
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}