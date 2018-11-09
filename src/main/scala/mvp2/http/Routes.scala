package mvp2.http

import akka.http.scaladsl.server.Directives.complete
import akka.actor.ActorSelection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import mvp2.data.{Blockchain, KeyBlock, Transaction}
import mvp2.messages.{CurrentBlockchainInfo, Get}
import mvp2.utils.Settings
import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.Json
import scala.concurrent.Future
import io.circe.generic.auto._
import io.circe.syntax._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import mvp2.utils.EncodingUtils._

case class Routes(settings: Settings, implicit val context: ActorRefFactory) extends FailFastCirceSupport {

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)

  val route: Route = getTxs ~ apiInfo ~ chainInfo
  val publisher: ActorSelection = context.actorSelection("/user/starter/blockchainer/publisher")
  val blockchainer: ActorSelection = context.actorSelection("/user/starter/blockchainer")
  val informator: ActorSelection = context.actorSelection("/user/starter/informator")

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson)(resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  def apiInfoF: Future[CurrentBlockchainInfo] = (informator ? Get).mapTo[CurrentBlockchainInfo]

  def apiInfo: Route = pathPrefix("info")(
    toJsonResponse(apiInfoF.map(_.asJson))
  )

  def getTxs: Route = path("sendTxs") {
    post(entity(as[List[Transaction]]) {
      txs =>
        complete {
          txs.foreach(tx => publisher ! tx)
          StatusCodes.OK
        }
    })
  }

  def chainInfoF: Future[List[KeyBlock]] = (blockchainer ? Get).mapTo[Blockchain].map(x => x.chain)

  def chainInfo: Route = path("chainInfo")(
    toJsonResponse(chainInfoF.map(_.asJson))
  )

}