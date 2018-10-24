import Actors.{Receiver, Sender, Starter}
import Messages.InfoMessage
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

object MVP2 extends App {
  println("Hello, world!")

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  system.actorOf(Props[Starter], "starter")
  system.actorOf(Props[Receiver], "Receiver")
  system.actorOf(Props[Sender],"Sender")
  system.actorSelection("/user/starter") ! InfoMessage("Hello, actor!")

}
