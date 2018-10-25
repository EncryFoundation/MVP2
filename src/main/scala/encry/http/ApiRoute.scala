package encry.http

import akka.http.scaladsl.server.Directives.complete
import encry.Messages.{CurrentBlockchainInfo, Get}
import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import encry.Utils.EncodingUtils._

case class ApiRoute(implicit val context: ActorRefFactory) {

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  def apiInfoVal: Future[CurrentBlockchainInfo] =
    (context.actorSelection("/user/starter/informator") ? Get).mapTo[CurrentBlockchainInfo]

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson) (resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  val apiInfo: Route = pathPrefix("info")(
    toJsonResponse(apiInfoVal.map(_.asJson))
  )
}