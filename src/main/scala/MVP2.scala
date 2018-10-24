import Actors.Starter
import Messages.InfoMessage
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContextExecutor

object MVP2 extends App with StrictLogging {
  println("Hello, world!")
  logger.info("Hello, world!")

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  system.actorOf(Props[Starter], "starter")
  system.actorSelection("/user/starter") ! InfoMessage("Hello, actor!")

}
