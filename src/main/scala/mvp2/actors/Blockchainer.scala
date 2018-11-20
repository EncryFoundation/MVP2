package mvp2.actors

import java.security.PublicKey

import mvp2.utils.ECDSA._
import akka.actor.{ActorRef, ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Period
import mvp2.data.InnerMessages.{CurrentBlockchainInfo, Get, TimeDelta}
import mvp2.data._
import mvp2.utils.Settings

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging {

  var blockchain: Blockchain = Blockchain()
  var currentDelta: Long = 0
  var nextTurn: Period = Period(KeyBlock(), settings)
  val keykeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant]), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props(classOf[Publisher], settings), "publisher")
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  val planner: ActorRef = context.actorOf(Props(classOf[Planner], settings), "planner")
  var waitingChainElements: Option[(Long, PublicKey)] = None

  override def receiveRecover: Receive = {
    case RecoveryCompleted => logger.info("Blockchainer completed recovery.")
  }

  override def receiveCommand: Receive = {
    case pair: (Long, PublicKey) => waitingChainElements = Some(pair)
      println(s"Blockchainer got new signature")
    case keyBlock: KeyBlock if sender() == publisher =>
      println(s"Blockchain got new block from publisher woth heigth: ${keyBlock.height}")
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
    case keyBlock: KeyBlock
      if verify(keyBlock.signature, keyBlock.getBytes, compressPublicKey(waitingChainElements.get._2)) &&
        nextTurn.begin >= System.currentTimeMillis() && System.currentTimeMillis() <= nextTurn.end =>
      println(s"Blockchain got new block from network woth heigth: ${keyBlock.height}")
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
    case TimeDelta(delta: Long) => currentDelta = delta
    case Get => sender ! blockchain
    case period: Period =>
      println(s"Blockchainer received period for new block with exact timestamp ${period.exactTime}.")
      nextTurn = period
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}