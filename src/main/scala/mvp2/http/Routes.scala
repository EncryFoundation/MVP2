package mvp2.http

import akka.http.scaladsl.server.Directives.complete
import akka.actor.ActorSelection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import mvp2.MVP2.system
import mvp2.data.Transaction
import mvp2.messages.{CurrentBlockchainInfo, Get}
import mvp2.utils.Settings
import akka.actor.ActorRefFactory
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
import akka.stream.ActorMaterializer
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import mvp2.utils.EncodingUtils._

case class Routes(settings: Settings, implicit val context: ActorRefFactory) extends FailFastCirceSupport {

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val route: Route = getTxs ~ apiInfo

  def apiInfoVal: Future[CurrentBlockchainInfo] =
    (context.actorSelection("/user/starter/informator") ? Get).mapTo[CurrentBlockchainInfo]

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson)(resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  def apiInfo: Route = pathPrefix("info")(
    toJsonResponse(apiInfoVal.map(_.asJson))
  )

  val publisher: ActorSelection = system.actorSelection("user/starter/blockchainer/publisher")

  def getTxs: Route = path("sendTx") {
    post(entity(as[List[Transaction]]) {
      txs =>
        complete {
          txs.foreach(tx => publisher ! tx)
          StatusCodes.OK
        }
    })
  }
}