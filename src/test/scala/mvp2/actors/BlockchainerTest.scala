package mvp2.actors

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpecLike}
import mvp2.actors.DummyTestBlockGenerator._
import mvp2.data.{Block, KeyBlock, MicroBlock}
import mvp2.messages.Get
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.duration._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContextExecutor

class BlockchainerTest extends TestKit(ActorSystem("BlockchainerTestSystem"))
  with PropSpecLike
  with Matchers
  with BeforeAndAfterAll
  with StrictLogging {

  property("Blockchain before and after save should be equals") {

    val blockchainer: ActorRef = system.actorOf(Props(classOf[Blockchainer]), "blockchainer")
    Thread.sleep(1000)
    val chain: TreeMap[Long, Block] = generateValidChain
    val sortedChain = chain.toSeq.sortBy(_._1)
    sortedChain.foreach {
      case x@(k: Long, v: MicroBlock) => blockchainer ! v
      case x@(k: Long, v: KeyBlock) => blockchainer ! v
    }
    Thread.sleep(2000)
    blockchainer ! PoisonPill
    Thread.sleep(1000)
    val blockchainerNew: ActorRef = system.actorOf(Props(classOf[Blockchainer]), "blockchainer")
    Thread.sleep(2000)

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val getRestoredChain = (blockchainerNew ? Get).mapTo[TreeMap[Long, Block]]
    getRestoredChain.map(x =>
      x.toSeq.sortBy(_._1) shouldEqual sortedChain.takeRight(6)
    )
  }
}