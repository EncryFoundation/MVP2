package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Period
import mvp2.data._
import mvp2.messages.CurrentBlockchainInfo
import mvp2.utils.Settings
import mvp2.messages.{Get, TimeDelta}

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging {

  var blockchain: Blockchain = Blockchain()
  var currentDelta: Long = 0L
  var nextTurn: Period = Period(KeyBlock(), settings)
  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant]), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props(classOf[Publisher], settings), "publisher")
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  val planner: ActorRef = context.actorOf(Props(classOf[Planner], settings), "planner")

  override def receiveRecover: Receive = {
    case RecoveryCompleted => logger.info("Blockchainer completed recovery.")
  }

  override def receiveCommand: Receive = {
    case keyBlock: KeyBlock =>
      blockchain = Blockchain(keyBlock :: blockchain.chain)
      informator ! CurrentBlockchainInfo(
        blockchain.chain.headOption.map(block => block.height).getOrElse(0),
        blockchain.chain.headOption,
        None
      )
      println(s"Blockchainer received new keyBlock with height ${keyBlock.height}. " +
        s"Blockchain's height is ${blockchain.chain.size}.")
      planner ! keyBlock
      publisher ! keyBlock
    case TimeDelta(delta: Long) => currentDelta = delta
    case Get => sender ! blockchain
    case period: Period =>
      logger.info(s"Blockchainer received period for new block with exact timestamp ${period.exactTime}.")
      nextTurn = period
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  /*
  def saveModifier(block: Block): Unit = if (block.isValid) block match {
    case keyBlock: KeyBlock =>
      logger.info(s"New keyBlock with height ${keyBlock.height} is received on blockchainer.")
      appendix.chain.foreach(block =>
        persist(block._2) { block =>
          logger.info(s"Block with id: ${encode2Base64(block.currentBlockHash)} and height ${block.height} is persisted.")
        })
      update(appendix.chain)
      appendix = appendix.copy(TreeMap(keyBlock.height -> keyBlock))
      accountant ! keyBlock
    case microBlock: MicroBlock =>
      logger.info(s"KeyBlock is valid with height ${microBlock.height}.")
      appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
      accountant ! microBlock
  }
  */

  def time: Long = System.currentTimeMillis() + currentDelta

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}