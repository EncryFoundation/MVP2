package mvp2.actors

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpecLike}
import mvp2.actors.DummyTestBlockGenerator._
import mvp2.data.{Block, KeyBlock, MicroBlock}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import mvp2.data.InnerMessages.Get
import mvp2.utils.Settings
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import scala.concurrent.duration._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContextExecutor

class BlockchainerTest extends TestKit(ActorSystem("BlockchainerTestSystem"))
  with PropSpecLike
  with Matchers
  with BeforeAndAfterAll
  with StrictLogging {

  property("Blockchain before and after save should be equals") {

    val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load)
      .as[Settings]("mvp")

    val blockchainer: ActorRef = system.actorOf(Props(classOf[Blockchainer], settings), "blockchainer")
    Thread.sleep(500)
    val chain: TreeMap[Long, Block] = generateValidChain
    val sortedChain = chain.toSeq.sortBy(_._1)
    sortedChain.foreach {
      case x@(k: Long, v: MicroBlock) => blockchainer ! v
      case x@(k: Long, v: KeyBlock) => blockchainer ! v
    }
    Thread.sleep(1000)
    blockchainer ! PoisonPill
    Thread.sleep(500)
    val blockchainerNew: ActorRef = system.actorOf(Props(classOf[Blockchainer], settings), "blockchainer1")
    Thread.sleep(1000)

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val getRestoredChain = (blockchainerNew ? Get).mapTo[TreeMap[Long, Block]]
    getRestoredChain.map(x =>
      x.toSeq.sortBy(_._1) shouldEqual sortedChain.takeRight(6)
    )
  }
}