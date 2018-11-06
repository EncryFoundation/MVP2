package mvp2.actors

import scala.concurrent.duration._
import scala.collection.immutable.{HashMap, TreeMap}
import scala.concurrent.{ExecutionContextExecutor, Future}
import mvp2.messages.Get
import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpecLike}
import mvp2.actors.DummyTestBlockGenerator._
import akka.util.{ByteString, Timeout}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import mvp2.utils.Settings
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import mvp2.data.{Block, KeyBlock, MicroBlock}
import akka.pattern.ask
import mvp2.actors.Accountant.Account
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

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load())
      .as[Settings]("mvp")

    val blockchainer: ActorRef = system.actorOf(Props(classOf[Blockchainer], settings), "blockChainActor")

    val chain: TreeMap[Long, Block] = generateChainOnlyKeyBlock
    chain.take(10).foreach {
      case x@(k: Long, v: KeyBlock) => blockchainer ! v
      case x@(k: Long, v: MicroBlock) => blockchainer ! v
    }

    Thread.sleep(100)

    val accountBeforeRestore: Future[(HashMap[ByteString, Account], ByteString)] =
      (system.actorSelection("/user/blockChainActor/accountant") ? Get)
        .mapTo[(HashMap[ByteString, Account], ByteString)]

    blockchainer ! chain.last._2

    Thread.sleep(500)

    blockchainer ! PoisonPill

    Thread.sleep(500)

    val blockchainerNew: ActorRef = system.actorOf(Props(classOf[Blockchainer], settings), "blockChainActorNew")
    Thread.sleep(2000)

    val getRestoredChain: Future[TreeMap[Long, Block]] = (blockchainerNew ? Get).mapTo[TreeMap[Long, Block]]

    val accountAfterRestore: Future[(HashMap[ByteString, Account], ByteString)] =
      (system.actorSelection("/user/blockChainActorNew/accountant") ? Get)
        .mapTo[(HashMap[ByteString, Account], ByteString)]

    Thread.sleep(2000)

    assert(getRestoredChain.futureValue == chain.take(10))
    assert(accountBeforeRestore.futureValue._1 == accountAfterRestore.futureValue._1)
    assert(accountBeforeRestore.futureValue._2 == accountAfterRestore.futureValue._2)
  }
}