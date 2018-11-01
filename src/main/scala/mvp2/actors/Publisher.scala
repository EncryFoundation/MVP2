package mvp2.actors

import java.security.KeyPair
import akka.actor.ActorSelection
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.utils.ECDSA
import scala.util.Random

class Publisher extends CommonActor {

  val blockchainer: ActorSelection = context.system.actorSelection("/user/starter/blockchainer")
  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  val randomizer: Random.type = scala.util.Random
  var currentDelta: NetworkTime.Time = 0L

  context.system.scheduler.schedule(10 second, 5 seconds)(createKeyBlock)

  context.system.scheduler.schedule(1 second, 3 seconds) {
    val randomData: ByteString = ByteString(randomizer.nextString(100))
    val pairOfKeys: KeyPair = ECDSA.createKeyPair
    val signature: ByteString = ECDSA.sign(pairOfKeys.getPrivate, randomData)
    self ! Transaction(ByteString(pairOfKeys.getPublic.toString), randomizer.nextLong(), signature, randomData)
  }

  context.system.scheduler.schedule(1 second, 10 seconds) {
    println(s"There are ${mempool.size} transactions in the mempool.")
  }

  override def preStart(): Unit = {
    logger.info("Starting the Publisher!")
    println(lastKeyBlock)
  }

  override def specialBehavior: Receive = {
    case transaction: Transaction => mempool = transaction :: mempool
    case keyBlock: KeyBlock => lastKeyBlock = keyBlock
    case delta: NetworkTime.Time =>
      logger.info(s"Update delta to: $delta")
      currentDelta = delta
  }

  def time: Long = System.currentTimeMillis() + currentDelta

  def createGenesysBlock(): KeyBlock = ???

  def createKeyBlock: KeyBlock = {
    val thisBlocksHeight: Long = lastKeyBlock.height + 1
    val currentTime: Long = System.currentTimeMillis
    val keyBlock: KeyBlock =
      KeyBlock(thisBlocksHeight, currentTime, lastKeyBlock.currentBlockHash, mempool)
    mempool = List.empty
    blockchainer ! keyBlock
    self ! keyBlock
    keyBlock
  }

}