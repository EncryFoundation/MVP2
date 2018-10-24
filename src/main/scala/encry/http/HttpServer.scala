package encry.http

import akka.http.scaladsl.Http
import encry.MVP2._

object HttpServer {

  def start(): Unit = {
    val (host, port): (String, Int) = ("0.0.0.0", 9051)
    Http().bindAndHandle(ApiRoute().apiInfo, host, port)
  }
}