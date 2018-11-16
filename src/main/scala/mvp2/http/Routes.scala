package mvp2.http

import akka.http.scaladsl.server.Directives.complete
import akka.actor.ActorSelection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import mvp2.data.{LightKeyBlock, Transaction}
import mvp2.messages.{CurrentBlockchainInfo, Get, GetLightChain}
import mvp2.utils.{EncodingUtils, Settings}
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
import akka.util.{ByteString, Timeout}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

case class Routes(settings: Settings, implicit val context: ActorRefFactory) extends FailFastCirceSupport {

  case class ApiInfo(height: Long = 0, keyBlock: Option[String], microBlock: Option[ByteString])

  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  val route: Route = getTxs ~ apiInfo ~ chainInfo
  val publisher: ActorSelection = context.actorSelection("/user/starter/blockchainer/publisher")
  val informator: ActorSelection = context.actorSelection("/user/starter/informator")

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson)(resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  def apiInfo: Route = pathPrefix("info")(
    toJsonResponse((informator ? Get).mapTo[CurrentBlockchainInfo].map(x =>
      ApiInfo(
        x.height,
        x.lastKeyBlock.map(block => EncodingUtils.encode2Base16(block.currentBlockHash)),
        x.lastMicroBlock).asJson
      )
    )
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

  def chainInfo: Route = path("chainInfo")(
    toJsonResponse((informator ? GetLightChain).mapTo[List[LightKeyBlock]].map(_.asJson))
  )
}