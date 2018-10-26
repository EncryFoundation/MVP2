import mvp2.Actors.Starter
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike

class AppTest extends TestKit(ActorSystem("TestAkkaSystem")) with WordSpecLike with BeforeAndAfterAll
  with DefaultTimeout with ImplicitSender {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  val StarterActorRef: ActorRef = system.actorOf(Props(classOf[Starter]), "starter")

  "App" should {
    "work" in {
      StarterActorRef ! 1
      expectNoMessage
    }
  }
}