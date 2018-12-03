package mvp2.actors

import java.security.KeyPair

import akka.actor.ActorSelection
import akka.util.ByteString
import mvp2.data.InnerMessages.{RequestForNewBlock, SyncingDone, TimeDelta}
import mvp2.data.NetworkMessages.Blocks
import mvp2.data.{KeyBlock, Mempool, Transaction}
import mvp2.utils.{ECDSA, EncodingUtils, Settings}

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class Publisher(settings: Settings) extends CommonActor {

  var lastKeyBlock: KeyBlock = KeyBlock()
  var myKeyPair: Option[KeyPair] = None
  val randomizer: Random.type = scala.util.Random
  var currentDelta: Long = 0
  //val testTxGenerator: ActorRef = context.actorOf(Props(classOf[TestTxGenerator]), "testTxGenerator")//TODO delete
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")
  var mempool: Mempool = Mempool(settings)

  context.system.scheduler.schedule(1.seconds, settings.mempoolSetting.mempoolCleaningTime.millisecond) {
    mempool.checkMempoolForInvalidTxs()
    logger.info(s"Mempool size is: ${mempool.mempool.size} after cleaning.")
  }

  override def preStart(): Unit =
    if (settings.otherNodes.isEmpty) context.become(publishBlockEnabled)

  override def specialBehavior: Receive = {
    case pair: KeyPair => myKeyPair = Some(pair)
    case SyncingDone =>
      logger.info("Syncing done!")
      context.become(publishBlockEnabled)
    case TimeDelta(delta: Long) =>
      logger.info(s"Update delta to: $delta")
      currentDelta = delta
  }

  def publishBlockEnabled: Receive = {
    case pair: KeyPair => myKeyPair = Some(pair)
    case transaction: Transaction =>
      if (mempool.updateMempool(transaction)) networker ! transaction
      logger.info(s"Mempool size is: ${mempool.mempool.size} after updating with new transaction.")
    case keyBlock: KeyBlock =>
      logger.info(s"Publisher received new lastKeyBlock with height ${keyBlock.height}.")
      networker ! keyBlock
      lastKeyBlock = keyBlock
      mempool.removeUsedTxs(keyBlock.transactions)
    case RequestForNewBlock(isFirstBlock, schedule) =>
      val newBlock: KeyBlock = createKeyBlock(isFirstBlock, schedule)
      logger.info(s"Publisher got new request and published block with height ${newBlock.height}.")
      context.parent ! Blocks(List(newBlock))
      networker ! newBlock
    case TimeDelta(delta: Long) =>
      logger.info(s"Update delta to: $delta")
      currentDelta = delta
    case tx => println(tx)
  }

  def time: Long = System.currentTimeMillis() + currentDelta

  def createKeyBlock(isFirstBlock: Boolean, schedule: List[ByteString]): KeyBlock = {
    logger.info(s"This is schedule for writing into new block ${schedule.map(EncodingUtils.encode2Base16).mkString(",")}")
    val currentTime: Long = time
    val keyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, currentTime, lastKeyBlock.currentBlockHash, mempool.mempool)
    val signedBlock: KeyBlock =
      if (isFirstBlock)
        keyBlock.copy(signature = ECDSA.sign(myKeyPair.get.getPrivate, keyBlock.getBytes), scheduler = schedule,
          publicKey = myKeyPair.map(x => ECDSA.compressPublicKey(x.getPublic)).getOrElse(ByteString.empty))
      else keyBlock.copy(signature = ECDSA.sign(myKeyPair.get.getPrivate, keyBlock.getBytes),
        publicKey = myKeyPair.map(x => ECDSA.compressPublicKey(x.getPublic)).getOrElse(ByteString.empty))
    KeyBlock(lastKeyBlock.height + 1, time, lastKeyBlock.currentBlockHash, List.empty)
    logger.info(s"New keyBlock with height ${keyBlock.height} is published by local publisher. " +
      s"${keyBlock.transactions.size} transactions inside.")
    mempool.cleanMempool()
    signedBlock
  }
}