package mvp2.actors

import java.text.SimpleDateFormat

import akka.actor.SupervisorStrategy.{Restart, Resume}
import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import akka.actor.{ActorRef, ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.{Epoch, Period}
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages.Blocks
import mvp2.data.InnerMessages.{CurrentBlockchainInfo, ExpectedBlockPublicKeyAndHeight, Get, TimeDelta}
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
  val accountant: ActorRef = context.actorOf(Props(classOf[Accountant]), "accountant")
  val networker: ActorRef = context.actorOf(Props(classOf[Networker], settings).withDispatcher("net-dispatcher")
    .withMailbox("net-mailbox"), "networker")
  val publisher: ActorRef = context.actorOf(Props(classOf[Publisher], settings), "publisher")
  val informator: ActorSelection = context.system.actorSelection("/user/starter/informator")
  val planner: ActorRef = context.actorOf(Props(classOf[Planner], settings), "planner")
  var expectedPublicKeyAndHeight: Option[ByteString] = None
  var epoch: Epoch = Epoch(List.empty)
  val df = new SimpleDateFormat("HH:mm:ss")

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
    case _: Exception => Restart
  }

  override def preStart(): Unit =
    if (settings.otherNodes.nonEmpty)
      context.system.scheduler.scheduleOnce(5 seconds)(networker !
        OwnBlockchainHeight(blockchain.chain.lastOption.map(_.height).getOrElse(-1)))

  override def receiveRecover: Receive = {
    case RecoveryCompleted => logger.info("Blockchainer completed recovery.")
  }

  override def receiveCommand: Receive = {
    case Blocks(blocks) =>
        blockCache += blocks
        applyBlockFromCache()
    case ExpectedBlockPublicKeyAndHeight(publicKey) =>
      expectedPublicKeyAndHeight = Some(publicKey)
      logger.info(s"Blockchainer got new public key " +
        s"${EncodingUtils.encode2Base16(expectedPublicKeyAndHeight.getOrElse(ByteString.empty))}")
    case TimeDelta(delta: Long) => currentDelta = delta
    case Get => sender ! blockchain
    case period: Period =>
      logger.info(s"Blockchainer received period for new block with exact timestamp ${df.format(period.begin)} -" +
        s" ${df.format(period.end)}.")
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
      planner ! block
      informator ! CurrentBlockchainInfo(
        blockchain.chain.lastOption.map(block => block.height).getOrElse(0),
        blockchain.chain.lastOption,
        None
      )
      logger.info(s"Blockchainer apply new keyBlock with height ${block.height}. " +
        s"Blockchain's height is ${blockchain.chain.size}.")
      if (!isSynced && blockchain.isSynced(settings.blockPeriod)) {
        isSynced = true
        logger.info(s"Synced done. Sent this message on the Planner and Publisher.")
        publisher ! SyncingDone
        planner ! SyncingDone
      }
      if (isSynced) publisher ! block
      applyBlockFromCache()
    case None =>
      networker ! OwnBlockchainHeight(blockchain.chain.lastOption.map(_.height).getOrElse(-1))
      logger.info("There is no applicable block in blocks cache")
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"
}