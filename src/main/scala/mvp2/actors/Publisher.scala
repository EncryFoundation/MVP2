package mvp2.actors

import java.security.KeyPair
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.utils.ECDSA
import scala.language.postfixOps
import scala.util.Random

class Publisher extends CommonActor {

  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: KeyBlock = KeyBlock()
  val randomizer: Random.type = scala.util.Random

  context.system.scheduler.schedule(10 second, 5 seconds)(createKeyBlock)

  context.system.scheduler.schedule(1 second, 3 seconds) {
    val randomData: ByteString = ByteString(randomizer.nextString(100))
    val pairOfKeys: KeyPair = ECDSA.createKeyPair
    val signature: ByteString = ECDSA.sign(pairOfKeys.getPrivate, randomData)
    self ! Transaction(ByteString(pairOfKeys.getPublic.toString), randomizer.nextLong(), signature, randomData)
  }

  override def specialBehavior: Receive = {
    case transaction: Transaction => mempool = transaction :: mempool
    case keyBlock: KeyBlock => lastKeyBlock = keyBlock
  }

  def createKeyBlock: KeyBlock = {
    val keyBlock: KeyBlock =
      KeyBlock(lastKeyBlock.height + 1, System.currentTimeMillis, lastKeyBlock.currentBlockHash, mempool)
    logger.info(s"New keyBlock with height ${keyBlock.height} is published by local publisher.")
    mempool = List.empty
    context.parent ! keyBlock
    self ! keyBlock
    keyBlock
  }
}