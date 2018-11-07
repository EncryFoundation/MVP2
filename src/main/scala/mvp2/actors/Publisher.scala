package mvp2.actors

import akka.actor.{ActorRef, ActorSelection, Props}
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.messages.Get
import mvp2.utils.Settings
import scala.language.postfixOps

class Publisher(settings: Settings) extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  //val testTxGenerator: ActorRef = context.actorOf(Props(classOf[TestTxGenerator]), "testTxGenerator")//TODO delete
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")

  override def specialBehavior: Receive = {
    case transaction: Transaction =>
      logger.info(s"Publisher received tx: $transaction and put it to the mempool.")
      mempool = transaction :: mempool
    case keyBlock: KeyBlock =>
      logger.info(s"Publisher received new lastKeyBlock with height ${keyBlock.height}.")
      lastKeyBlock = keyBlock
    case Get =>
      val newBlock: KeyBlock = createKeyBlock
      println(s"Publisher got new request and published block with height ${newBlock.height}.")
      context.parent ! newBlock
      networker ! newBlock
  }

  def createKeyBlock: KeyBlock = {
    val keyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, System.currentTimeMillis, lastKeyBlock.currentBlockHash, mempool)
    //logger.info(s"${mempool.size} transactions in the mempool.")
    println(s"New keyBlock with height ${keyBlock.height} is published by local publisher. " +
      s"${keyBlock.transactions.size} transactions inside.")
    mempool = List.empty
    //logger.info(s"${mempool.size} transactions in the mempool.")
    keyBlock
  }
}