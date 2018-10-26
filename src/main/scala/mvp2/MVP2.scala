package mvp2

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import mvp2.Actors.Starter

object MVP2 extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  system.actorOf(Props[Starter], "starter")

}