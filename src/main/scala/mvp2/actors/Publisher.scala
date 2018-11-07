package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.messages.Get
import scala.language.postfixOps
import mvp2.messages.TimeDelta
import scala.util.Random

class Publisher extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  val randomizer: Random.type = scala.util.Random
  var currentDelta: Long = 0L
  val testTxGenerator: ActorRef = context.actorOf(Props(classOf[TestTxGenerator]), "testTxGenerator")//TODO delete
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")

  context.system.scheduler.schedule(10 second, 5 seconds)(createKeyBlock)

  override def specialBehavior: Receive = {
    case transaction: Transaction =>
      logger.info(s"Publisher received tx: $transaction and put it to the mempool.")
      mempool = transaction :: mempool
    case keyBlock: KeyBlock => lastKeyBlock = keyBlock
    case Get =>
      val newBlock: KeyBlock = createKeyBlock
      lastKeyBlock = newBlock
      context.parent ! newBlock
      networker ! newBlock
    case TimeDelta(delta: Long) => currentDelta = delta
  }

  def time: Long = System.currentTimeMillis() + currentDelta

  def createGenesisBlock(): KeyBlock = ???

  def createKeyBlock: KeyBlock = {
    val keyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, System.currentTimeMillis, lastKeyBlock.currentBlockHash, mempool)
    logger.info(s"${mempool.size} transactions in the mempool.")
    logger.info(s"New keyBlock with height ${keyBlock.height} is published by local publisher. " +
      s"${keyBlock.transactions.size} transactions inside.")
    mempool = List.empty
    logger.info(s"${mempool.size} transactions in the mempool.")
    keyBlock
  }

}