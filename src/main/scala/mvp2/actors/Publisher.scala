package mvp2.actors

import java.security.KeyPair
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.utils.{ECDSA, Sha256}
import scala.util.Random

class Publisher extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  val randomizer: Random.type = scala.util.Random

  context.system.scheduler.schedule(1 second, 5 seconds)(createKeyBlock)

  context.system.scheduler.schedule(1 second, 3 seconds){
    val randomData: ByteString = ByteString(randomizer.nextString(100))
    val pairOfKeys: KeyPair = ECDSA.createKeyPair
    val signature = ECDSA.sign(pairOfKeys.getPrivate, randomData)
    self ! Transaction(ByteString(randomizer.nextString(100)), randomizer.nextLong(), signature,randomData)
  }

  override def preStart(): Unit = {
    logger.info("Starting the Publisher!")
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
    val keyBlock: KeyBlock = KeyBlock(thisBlocksHeight, currentTime, Sha256.toSha256(text), mempool)
    mempool = List.empty
    println(keyBlock)
    self ! keyBlock
    keyBlock
  }

}