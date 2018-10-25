package encry.http

import akka.http.scaladsl.Http
import encry.MVP2._
import encry.Utils.Settings

object HttpServer {

  def start(): Unit = {
    val (host, port): (String, Int) = (Settings.settings.httpHost, Settings.settings.httpPort)
    Http().bindAndHandle(ApiRoute().apiInfo, host, port)
  }
}