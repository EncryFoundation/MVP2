package mvp2.actors

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{ActorRef, ActorSelection, OneForOneStrategy, Props, SupervisorStrategy}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Period
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages.Blocks
import mvp2.data._
import mvp2.utils.Settings
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging {

  var blockchain: Blockchain = Blockchain()
  var blockCache: BlocksCache = BlocksCache()
  var currentDelta: Long = 0
  var nextTurn: Period = Period(KeyBlock(), settings)
  var isSynced: Boolean = settings.otherNodes.isEmpty
  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant]), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props(classOf[Publisher], settings), "publisher")
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  val planner: ActorRef = context.actorOf(Props(classOf[Planner], settings), "planner")

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
    case _: Exception => Resume
  }

  override def preStart(): Unit =
    if (settings.otherNodes.nonEmpty)
      context.system.scheduler.scheduleOnce(2 seconds)(networker !
        OwnBlockchainHeight(blockchain.chain.lastOption.map(_.height).getOrElse(-1)))

  override def receiveRecover: Receive = {
    case RecoveryCompleted => logger.info("Blockchainer completed recovery.")
  }

  override def receiveCommand: Receive = {
    case Blocks(blocks) =>
      if (blocks.headOption.exists(_.height > blockchain.maxHeight)) {
        blockCache += blocks
        applyBlockFromCache()
      }
    case keyBlock: KeyBlock =>
      blockCache += keyBlock
      applyBlockFromCache()
    case TimeDelta(delta: Long) => currentDelta = delta
    case Get => sender ! blockchain
    case period: Period =>
      logger.info(s"Blockchainer received period for new block with exact timestamp ${period.exactTime}.")
      nextTurn = period
    case CheckRemoteBlockchain(remoteHeight, remote) =>
      blockchain.getMissingPart(remoteHeight).foreach(blocks =>
        networker ! RemoteBlockchainMissingPart(blocks.take(settings.network.maxBlockQtyInBlocksMessage).toList, remote)
      )
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  def applyBlockFromCache(): Unit = blockCache.getApplicableBlock(blockchain) match {
    case Some(block) =>
      blockchain += block
      blockCache -= block
      informator ! CurrentBlockchainInfo(
        blockchain.chain.lastOption.map(block => block.height).getOrElse(0),
        blockchain.chain.lastOption,
        None
      )
      logger.info(s"Blockchainer apply new keyBlock with height ${block.height}. " +
        s"Blockchain's height is ${blockchain.chain.size}.")
      planner ! block
      publisher ! block
      if (blockCache.isEmpty && !isSynced) {
        isSynced = true
        publisher ! SyncingDone
      }
      applyBlockFromCache()
    case None =>
      networker ! OwnBlockchainHeight(blockchain.chain.lastOption.map(_.height).getOrElse(-1))
      logger.info("There is no applicable block in blocks cache")
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}