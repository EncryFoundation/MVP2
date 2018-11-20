package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Period
import mvp2.data.InnerMessages.{CurrentBlockchainInfo, Get, TimeDelta}
import mvp2.data._
import mvp2.messages.CurrentBlockchainInfo
import mvp2.messages.Get
import mvp2.utils.EncodingUtils._
import mvp2.utils.Settings

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging {

  var blockchain: Blockchain = Blockchain()
  var currentDelta: Long = 0
  var nextTurn: Period = Period(KeyBlock(), settings)
  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant], settings.postgres.exists(_.write)), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props(classOf[Publisher], settings), "publisher")
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  val planner: ActorRef = context.actorOf(Props(classOf[Planner], settings), "planner")

  override def receiveRecover: Receive = {
    case RecoveryCompleted => logger.info("Blockchainer completed recovery.")
  }

  override def receiveCommand: Receive = {
    case RecoveryCompleted if settings.postgres.exists(_.read) => publisher ! lastKeyBlock.getOrElse(
      KeyBlock(0, System.currentTimeMillis(), ByteString.empty, List())
    )
    case keyBlock: KeyBlock =>
      blockchain = Blockchain(keyBlock :: blockchain.chain)
      informator ! CurrentBlockchainInfo(
        blockchain.chain.headOption.map(block => block.height).getOrElse(0),
        blockchain.chain.headOption,
        None
      )
      logger.info(s"Blockchainer received new keyBlock with height ${keyBlock.height}. " +
        s"Blockchain consists of ${blockchain.chain.size} blocks.")
      planner ! keyBlock
      publisher ! keyBlock
      if (settings.postgres.exists(_.write)) context.actorSelection("/user/starter/pgWriter") ! block
    case TimeDelta(delta: Long) => currentDelta = delta
    case Get => sender ! blockchain
    case period: Period =>
      logger.info(s"Blockchainer received period for new block with exact timestamp ${period.exactTime}.")
      nextTurn = period
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}