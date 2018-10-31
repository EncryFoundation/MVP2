package mvp2.actors

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.utils.Sha256

class Publisher extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()

  context.system.scheduler.schedule(1 second, 5 seconds)(createKeyBlock)

  override def preStart(): Unit = {
    println(lastKeyBlock)
  }

  override def specialBehavior: Receive = {
    case transaction: Transaction if transaction.isValid => transaction :: mempool
    case keyBlock: KeyBlock => lastKeyBlock = keyBlock
  }

  def createGenesysBlock(): KeyBlock = ???

  def createKeyBlock: KeyBlock = {
    val thisBlocksHeight: Long = lastKeyBlock.height + 1
    val currentTime: Long = System.currentTimeMillis
    val text: String = thisBlocksHeight.toString + currentTime.toString + lastKeyBlock.previousGeneralBlock.toString
    val keyBlock: KeyBlock = KeyBlock(thisBlocksHeight, currentTime, Sha256.toSha256(text))
    println(keyBlock)
    self ! keyBlock
    keyBlock
  }
}