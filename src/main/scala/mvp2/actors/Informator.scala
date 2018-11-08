package mvp2.actors

import akka.actor.ActorRefFactory
import mvp2.messages.Get
import mvp2.http.{ApiRoute, TransactionRoute}
import mvp2.messages.CurrentBlockchainInfo
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import mvp2.MVP2._
import mvp2.utils.Settings

class Informator(settings: Settings) extends CommonActor {

  Informator.start(settings, context)

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo()

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo => actualInfo = currentBlockchainInfo
  }
}

object Informator {

  def routesCompose(settings: Settings, context: ActorRefFactory): Seq[Route] = {
    val routes: Seq[Route] = Seq(ApiRoute(settings, context).apiInfo, TransactionRoute(settings, context).route)
    val result = routes.reduce(x => x ~ _)
  }

  def start(settings: Settings, context: ActorRefFactory): Unit = Http().bindAndHandle(
    ApiRoute(settings, context).apiInfo ~ TransactionRoute(settings, context).route,
    settings.apiSettings.httpHost,
    settings.apiSettings.httpPort
  )
}