package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import mvp2.data.{KeyBlock, Transaction}
import mvp2.messages.Get
import mvp2.utils.Settings
import scala.language.postfixOps
import mvp2.messages.TimeDelta
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Publisher(settings: Settings) extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  val randomizer: Random.type = scala.util.Random
  var currentDelta: Long = 0
  val testTxGenerator: ActorRef = context.actorOf(Props(classOf[TestTxGenerator]), "testTxGenerator")
  //TODO delete
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")

  context.system.scheduler.schedule(1.seconds, settings.mempoolSetting.mempoolSharedTime.seconds) {
    mempool = cleanMempool(mempool)
    mempool.foreach(tx => networker ! tx)
    println(s"${mempool.size}, $mempool")
  }

  override def specialBehavior: Receive = {
    case transaction: Transaction => mempool = updateMempool(transaction, mempool)
    case keyBlock: KeyBlock =>
      logger.info(s"Publisher received new lastKeyBlock with height ${keyBlock.height}.")
      context.actorSelection("/user/starter/blockchainer/networker") ! keyBlock
      lastKeyBlock = keyBlock
      mempool = checkTxsFromNewBlock(keyBlock.transactions, mempool)
    case Get =>
      val newBlock: KeyBlock = createKeyBlock
      println(s"Publisher got new request and published block with height ${newBlock.height}.")
      context.parent ! newBlock
      networker ! newBlock
    case TimeDelta(delta: Long) =>
      logger.info(s"Update delta to: $delta")
      currentDelta = delta
  }

  def time: Long = System.currentTimeMillis() + currentDelta

  def createKeyBlock: KeyBlock = {
    val keyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, time, lastKeyBlock.currentBlockHash, mempool)
    println(s"New keyBlock with height ${keyBlock.height} is published by local publisher. " +
      s"${keyBlock.transactions.size} transactions inside.")
   // mempool = List.empty
    keyBlock
  }

  def updateMempool(transaction: Transaction, mempool: List[Transaction]): List[Transaction] =
    if (!mempool.contains(transaction)) {
      println(s"Got new transaction $transaction in mempool.")
      transaction :: mempool
    } else {
      println(s"This transaction is already in mempool.")
      mempool
    }

  def checkTxsFromNewBlock(transactions: List[Transaction], mempool: List[Transaction]): List[Transaction] =
    mempool.diff(transactions)

  def cleanMempool(mempool: List[Transaction]): List[Transaction] =
    mempool.filter(tx => tx.timestamp > System.currentTimeMillis() - settings.mempoolSetting.transactionsValidTime)
}