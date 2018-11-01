package mvp2.http

import akka.http.scaladsl.server.Directives.complete
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
import mvp2.Messages.{CurrentAccountantInfo, GetAccountantInfo}
import mvp2.Utils.Settings

import scala.concurrent.{ExecutionContextExecutor, Future}

case class ApiRouteAccountant(settings: Settings, implicit val context: ActorRefFactory) {
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)

  def apiInfoVal: Future[CurrentAccountantInfo] =
    (context.actorSelection("/user/starter/informator") ? GetAccountantInfo).mapTo[CurrentAccountantInfo]

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson) (resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  val apiInfo: Route = pathPrefix("accountant")(
    toJsonResponse(apiInfoVal.map(_.asJson))
  )
}
