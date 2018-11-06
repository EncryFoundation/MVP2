package mvp2.actors

import akka.util.{ByteString, Timeout}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import mvp2.utils.Settings
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration._
import scala.collection.immutable.{HashMap, TreeMap}
import scala.concurrent.{ExecutionContextExecutor, Future}
import mvp2.messages.{CurrentState, Get}
import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{ActorRef, ActorSelection, ActorSystem, PoisonPill, Props}
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpecLike}
import mvp2.actors.DummyTestBlockGenerator._
import mvp2.data.{Block, KeyBlock, MicroBlock}
import akka.pattern.ask
import org.scalatest.concurrent.ScalaFutures

class BlockchainerTest extends TestKit(ActorSystem("BlockChainSystemTest"))
  with PropSpecLike
  with BeforeAndAfterAll
  with Matchers
  with StrictLogging
  with ImplicitSender
  with ScalaFutures {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  property("BlockChain before and after save should be equals") {

    val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load())
      .as[Settings]("mvp")

    val blockchainer: ActorRef = system.actorOf(Props(classOf[Blockchainer], settings), "blockChainActor")

    println(blockchainer)
    Thread.sleep(1000)
    val chain: TreeMap[Long, Block] = generateChainOnlyKeyBlock
    chain.foreach(x => println(x._2))
    chain.take(10).foreach {
      case x@(k: Long, v: KeyBlock) =>
        blockchainer ! v
        println(s"Sent KeyBlock to chain with height: ${v.height}")
      case x@(k: Long, v: MicroBlock) =>
        blockchainer ! v
        println(s"Sent MicroBlock to chain with height: ${v.height}")
    }
    Thread.sleep(1000)

//    val a = (system.actorSelection("/user/blockChainActor/accountant") ? Get).mapTo[CurrentState]

    val accountBeforeRestore: Future[(ByteString, HashMap[ByteString, Accountant.Account])] =
      (blockchainer ? Get).mapTo[CurrentState].map(x => x.stateRoot -> x.accInfo)

    blockchainer ! chain.last._2
    println(s"Sent KeyBlock to chain with height: ${chain.last._2.height}")

    Thread.sleep(1000)
    blockchainer ! PoisonPill
    Thread.sleep(1000)
    lazy val blockchainerNew: ActorRef = system.actorOf(Props(classOf[Blockchainer], settings), "blockChainActorNew")
    Thread.sleep(1000)

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val getRestoredChain: Future[TreeMap[Long, Block]] = (blockchainerNew ? Get).mapTo[TreeMap[Long, Block]]
    val accountAfterRestore: Future[(ByteString, HashMap[ByteString, Accountant.Account])] =
      (blockchainerNew ? Get).mapTo[CurrentState].map(x => x.stateRoot -> x.accInfo)

    Thread.sleep(1000)

    for {
      chain <- getRestoredChain
    } yield chain.foreach(x => println(x._2 + " from test"))

    chain.take(10).foreach(x => println(x._2 + " local"))

    Thread.sleep(1000)

    assert(getRestoredChain.futureValue == chain.take(10))
    assert(accountBeforeRestore.futureValue._2 == accountAfterRestore.futureValue._2)
    assert(accountBeforeRestore.futureValue._1 == accountAfterRestore.futureValue._1)

  }
}