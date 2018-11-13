package mvp2.http

import java.nio.file.Path
import akka.http.scaladsl.server.Directives.complete
import akka.actor.ActorSelection
import akka.http.scaladsl.model._
import mvp2.data.{LightKeyBlock, Transaction}
import mvp2.messages.{CurrentBlockchainInfo, Get, GetLightChain}
import mvp2.utils.Settings
import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Route
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import better.files.File
import better.files._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.util.{Failure, Success, Try}

case class Routes(settings: Settings, implicit val context: ActorRefFactory) extends FailFastCirceSupport {

  case class ApiInfo(height: Long = 0, keyBlock: Option[ByteString], microBlock: Option[ByteString])

  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  val routes: Seq[Route] =
    if (settings.apiSettings.enableStateDownload) Seq(getTxs, apiInfo, chainInfo, downloadState)
    else Seq(getTxs, apiInfo, chainInfo)

  val route: Route = routes.reduce(_ ~ _)
  val publisher: ActorSelection = context.actorSelection("/user/starter/blockchainer/publisher")
  val informator: ActorSelection = context.actorSelection("/user/starter/informator")

  def toJsonResponse(fJson: Future[Json]): Route = onSuccess(fJson)(resp =>
    complete(HttpEntity(ContentTypes.`application/json`, resp.spaces2))
  )

  def apiInfo: Route = pathPrefix("info")(
    toJsonResponse((informator ? Get).mapTo[CurrentBlockchainInfo].map(x =>
      ApiInfo(x.height, x.lastKeyBlock.map(block => block.currentBlockHash), x.lastMicroBlock).asJson)
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

  def downloadState: Route = (path("download") & get)(
    createZip match {
      case Success(path) => getFromFile(path.toFile, MediaTypes.`application/zip`)
      case Failure(th) =>
        th.printStackTrace()
        complete(HttpResponse(InternalServerError))
    }
  )

  private def createZip: Try[Path] = Try {
    file"./state.zip".delete(true)
    file"./tmp/".delete(true)
    val dir: File = file"./tmp/".createDirectory()
    File("./leveldb").list.map(_.copyToDirectory(dir))
    val zip: File = file"./state.zip".deleteOnExit()
    dir.zipTo(file"./state.zip").deleteOnExit()
    zip.path
  }

}