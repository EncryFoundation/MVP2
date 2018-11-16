package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import mvp2.data.{KeyBlock, Transaction}
import mvp2.messages.{Get, SyncingDone, TimeDelta}
import mvp2.utils.Settings

import scala.language.postfixOps
import scala.util.Random

class Publisher(settings: Settings) extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  val randomizer: Random.type = scala.util.Random
  var currentDelta: Long = 0
  //val testTxGenerator: ActorRef = context.actorOf(Props(classOf[TestTxGenerator]), "testTxGenerator")//TODO delete
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")

  override def specialBehavior: Receive = {
    case SyncingDone =>
      logger.info("Syncing done!")
      context.become(syncingDone)
  }

  def syncingDone: Receive = {
    case transaction: Transaction =>
      logger.info(s"Publisher received tx: $transaction and put it to the mempool.")
      mempool = transaction :: mempool
    case keyBlock: KeyBlock =>
      logger.info(s"Publisher received new lastKeyBlock with height ${keyBlock.height}.")
      context.actorSelection("/user/starter/blockchainer/networker") ! keyBlock
      lastKeyBlock = keyBlock
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
    mempool = List.empty
    keyBlock
  }
}