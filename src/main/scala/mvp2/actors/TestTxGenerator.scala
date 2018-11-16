package mvp2.actors

import java.security.KeyPair
import scala.concurrent.duration._
import scala.util.Random
import scala.language.postfixOps
import akka.util.ByteString
import mvp2.data.Transaction
import mvp2.utils.ECDSA
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global

class TestTxGenerator extends CommonActor {

  val randomizer: Random.type = scala.util.Random

  val clients: Seq[(Int, KeyPair)] = for (i <- 1 to 5) yield (i, ECDSA.createKeyPair)

  context.system.scheduler.schedule(1 second, 5 seconds) {
    val randomData: ByteString = ByteString(randomizer.nextString(100))
    val client: KeyPair = clients.find(_._1 == randomizer.nextInt(5) + 1).getOrElse((0, ECDSA.createKeyPair))._2
    val signature: ByteString = ECDSA.sign(client.getPrivate, randomData)
    val transaction: Transaction =
      Transaction(ByteString(client.getPublic.toString), System.currentTimeMillis(),
        randomizer.nextLong(), signature, randomData)
    self ! transaction
  }

  override def specialBehavior: Receive = {
    case transaction: Transaction =>
      logger.info(s"TestTxGenerator created tx with publicKey: ${transaction.publicKey}.")
      context.parent ! transaction
  }

}
