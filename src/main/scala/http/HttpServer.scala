package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object HttpServer {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def start(): Unit = {
    val (host, port): (String, Int) = ("0.0.0.0", 9051)
    Http().bindAndHandle(ApiRoute().apiInfo, host, port)
  }
}