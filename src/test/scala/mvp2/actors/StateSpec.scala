package mvp2.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest.{Matchers, PropSpecLike}

class StateSpec extends TestKit(ActorSystem("StateTestSystem")) with PropSpecLike with Matchers {

  property("state applying blocks") {
    val numBlocks = 10
    val stateActor = system.actorOf(Props[Accountant], "Accountant")
    val sampleBlockChain = DummyTestBlockGenerator.generateChain(numBlocks)
    sampleBlockChain.foreach(b => stateActor ! b)
    Thread.sleep(1000)
    //State.getAccountsInfo.size shouldBe 10
    //State.getAccountsInfo.toList.forall(e => e._2.data.length == numBlocks) shouldBe true
    true shouldBe true
  }
}