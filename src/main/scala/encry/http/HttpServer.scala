package encry.http

import akka.http.scaladsl.Http
import encry.MVP2._
import encry.Utils.Settings

object HttpServer {
  def start(): Unit = Http().bindAndHandle(ApiRoute().apiInfo, Settings.settings.httpHost, Settings.settings.httpPort)

}