package mvp2.actors

import java.net.InetSocketAddress
import java.security.KeyPair
import akka.actor.ActorSelection
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.data.{KeyBlock, Transaction}
import mvp2.messages.PublishBlock
import mvp2.utils.ECDSA
import scala.util.Random

class Publisher extends CommonActor {

  val blockchainer: ActorSelection = context.system.actorSelection("/user/starter/blockchainer")
  var mempool: List[Transaction] = List.empty
  var lastKeyBlock: Option[KeyBlock] = None
  val randomizer: Random.type = scala.util.Random
  var schedule: List[InetSocketAddress] = List.empty

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
    case keyBlock: KeyBlock => lastKeyBlock = Some(keyBlock)
    case PublishBlock =>
      logger.info("Should create block!")
      createKeyBlock
  }

  lazy val genesisBlock: KeyBlock = KeyBlock(0L, 0L, ByteString.empty, List.empty)

  def createKeyBlock: Unit = {
    val thisBlocksHeight: Long = lastKeyBlock.map(_.height).getOrElse(0L) + 1
    val currentTime: Long = System.currentTimeMillis
    val keyBlock: KeyBlock =
      KeyBlock(
        thisBlocksHeight,
        currentTime,
        lastKeyBlock.getOrElse(genesisBlock).hash,
        mempool
      )
    mempool = List.empty
    blockchainer ! keyBlock
    self ! keyBlock
  }

}