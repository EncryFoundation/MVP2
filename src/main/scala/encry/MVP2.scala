package encry

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer

object MVP2 extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  system.actorOf(Props[Starter], "starter")

}