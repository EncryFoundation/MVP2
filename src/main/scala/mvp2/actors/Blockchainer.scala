package mvp2.actors

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import akka.actor.{ActorRef, ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Period
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages.Blocks
import mvp2.data.InnerMessages.{CurrentBlockchainInfo, ExpectedBlockSignatureAndHeight, Get, TimeDelta}
import mvp2.data._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import mvp2.utils.{EncodingUtils, Settings}

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging {

  var blockchain: Blockchain = Blockchain()
  var blockCache: BlocksCache = BlocksCache()
  var currentDelta: Long = 0
  var nextTurn: Period = Period(KeyBlock(), settings)
  var isSynced: Boolean = settings.otherNodes.isEmpty
  val keykeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant]), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props(classOf[Publisher], settings), "publisher")
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  val planner: ActorRef = context.actorOf(Props(classOf[Planner], settings), "planner")
  var expectedBlockSignatureAndHeight: Option[(Long, ByteString)] = None

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
        blockCache += blocks
        applyBlockFromCache()
    case ExpectedBlockSignatureAndHeight(height, signature) =>
      expectedBlockSignatureAndHeight = Some(height, signature)
      logger.info(s"Blockchainer got new signature " +
        s"${EncodingUtils.encode2Base16(expectedBlockSignatureAndHeight.map(_._2).getOrElse(ByteString.empty))}")
    case TimeDelta(delta: Long) => currentDelta = delta
    case Get => sender ! blockchain
    case period: Period =>
      logger.info(s"Blockchainer received period for new block with exact timestamp ${period.begin} ${period.end}.")
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
        planner ! SyncingDone
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