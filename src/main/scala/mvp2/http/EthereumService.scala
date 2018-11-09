package mvp2.http

import java.util.concurrent.TimeUnit
import akka.actor.ActorSelection
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe.Json
import io.circe.parser.parse
import mvp2.MVP2.system
import mvp2.utils.EthRequestType.EthRequestType
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object EthereumService {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val anchorer: ActorSelection = system.actorSelection("user/starter/anchorer")

  def sendRequestToEthereum(innerId: String, requestBody: Json, peerRPCAddress: String, requestType: EthRequestType): Unit = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = peerRPCAddress,
        entity = HttpEntity(ContentTypes.`application/json`, requestBody.toString)
      ))
    responseFuture.onComplete{
      case Success(response) => response.status match {
        case StatusCodes.OK if response.entity.contentType == ContentTypes.`application/json` =>
          Unmarshal(response.entity).to[String].onComplete {
            case Success(s) => parse(s) match {
              case Right(json) => anchorer ! EthResponse(innerId, requestType, json)
              case Left(_) => ???
            }
            case Failure(e) => ???
          }
      }
      case Failure(e) => ???
    }
    Await.result(responseFuture, Duration.apply(4, TimeUnit.SECONDS))
  }
}

case class EthResponse(innerId: String, rtype: EthRequestType, responseBody: Json)
