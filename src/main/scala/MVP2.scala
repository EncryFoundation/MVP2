import Actors.TestActor
import Actors.TestActor.Message
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContextExecutor

object MVP2 extends App {
  println("Hello, world!")

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  system.actorOf(Props[TestActor], "testActor")
  system.actorSelection("/user/testActor") ! Message("Hello, actor!")

}
