package encry.http

import akka.http.scaladsl.server.Directives.complete
import encry.Messages.{CurrentBlockchainInfo, Get}
import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import io.circe.Json
import io.circe.syntax._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

case class ApiRoute(implicit val context: ActorRefFactory) {

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  val apiInfoVal: Future[CurrentBlockchainInfo] =
    (context.actorSelection("/user/starter/informator") ? Get).mapTo[CurrentBlockchainInfo]

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson) (resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  def mapInfo(info: CurrentBlockchainInfo): Json = Map(
    "height" -> info.height.asJson,
    "lastMicroBlock" -> info.lastMicroBlock.getOrElse(ByteString.empty).toString().asJson,
    "lastGeneralBlock" -> info.lastGeneralBlock.getOrElse(ByteString.empty).toString().asJson
  ).asJson

  val apiInfo: Route = pathPrefix("info")(
    toJsonResponse(apiInfoVal.map(x => mapInfo(x)))
  )
}