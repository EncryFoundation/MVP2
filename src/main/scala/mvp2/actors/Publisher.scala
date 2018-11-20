package mvp2.actors

import java.security.{KeyPair, PublicKey}
import akka.actor.{ActorRef, ActorSelection, Props}
import akka.util.ByteString
import mvp2.data.InnerMessages.{Get, MyPublicKey, TimeDelta}
import mvp2.data.{KeyBlock, Mempool, Transaction}
import mvp2.utils.{ECDSA, Settings}
import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Publisher(settings: Settings) extends CommonActor {

  var lastKeyBlock: KeyBlock = KeyBlock()
  var mySignature: Option[KeyPair] = None
  val randomizer: Random.type = scala.util.Random
  var currentDelta: Long = 0
  val testTxGenerator: ActorRef = context.actorOf(Props(classOf[TestTxGenerator]), "testTxGenerator")//TODO delete
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")
  var mempool: Mempool = Mempool(settings)

  context.system.scheduler.schedule(1.seconds, settings.mempoolSetting.mempoolCleaningTime.millisecond) {
    mempool.checkMempoolForInvalidTxs()
    logger.info(s"Mempool size is: ${mempool.mempool.size} after cleaning.")
  }

  override def specialBehavior: Receive = {
    case pair: KeyPair => mySignature = Some(pair)
    case transaction: Transaction =>
      if (mempool.updateMempool(transaction)) networker ! transaction
      logger.info(s"Mempool size is: ${mempool.mempool.size} after updating with new transaction.")
    case keyBlock: KeyBlock =>
      logger.info(s"Publisher received new lastKeyBlock with height ${keyBlock.height}.")
      networker ! keyBlock
      lastKeyBlock = keyBlock
      mempool.removeUsedTxs(keyBlock.transactions)
    case Get =>
      val newBlock: KeyBlock = createKeyBlock
      logger.info(s"Publisher got new request and published block with height ${newBlock.height}.")
      context.parent ! newBlock
      networker ! newBlock
    case TimeDelta(delta: Long) =>
      logger.info(s"Update delta to: $delta")
      currentDelta = delta
  }

  def time: Long = System.currentTimeMillis() + currentDelta

  def createKeyBlock: KeyBlock = {
    val timeM = time
    val keyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, timeM, lastKeyBlock.currentBlockHash)
    val myNSignature: ByteString = ECDSA.sign(mySignature.get.getPrivate, keyBlock.getBytes)
    val newKeyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, timeM, lastKeyBlock.currentBlockHash, mempool.mempool, signature = myNSignature)
    logger.info(s"New keyBlock with height ${keyBlock.height} is published by local publisher. " +
      s"${keyBlock.transactions.size} transactions inside.")
    mempool.cleanMempool()
    newKeyBlock
  }
}