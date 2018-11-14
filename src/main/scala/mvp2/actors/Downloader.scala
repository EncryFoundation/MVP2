package mvp2.actors

import java.nio.file.Paths
import akka.pattern.pipe
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.scaladsl.FileIO
import better.files.File
import better.files._
import scala.util.{Failure, Success}
import mvp2.MVP2._
import scala.concurrent.ExecutionContext.Implicits.global

class Downloader(downloadFrom: String) extends CommonActor {
  override def specialBehavior: Receive = ???

  override def preStart(): Unit = self ! downloadFrom

  override def receive: Receive = {
    case address: String =>
      pipe(Http(context.system).singleRequest(HttpRequest(uri = s"http://$address/download"))).to(self)
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runWith(FileIO.toPath(Paths.get(s"./state.zip"))).onComplete {
        case Success(_) =>
          unzip()
          println(1)
          context.parent ! DownloadComplete
        case Failure(th) =>
          th.printStackTrace()
          context.parent ! DownloadComplete
          context.stop(self)
      }
    case resp @ HttpResponse(code, _, _, _) =>
      println(code)
      resp.discardEntityBytes()
      context.parent ! DownloadComplete
      context.stop(self)
  }

  def unzip(): Unit = {
    def moveAllFiles(from: File, dest: File): Unit = from.list.foreach(_.copyToDirectory(dest))
    val zip: File = file"./encry.zip"
    val unzipped: File = zip.unzipTo(file"./state")
    zip.delete(true)
    file"./leveldb/journal".delete(true)
    file"./leveldb/snapshots".delete(true)
    val unzippedJournal: File = file"./state/journal"
    val unzippedSnapshots: File = file"./state/snapshots"
    val journalDir: File = file"./leveldb/journal".createDirectories()
    val snapshotsDir: File = file"./leveldb/snapshots".createDirectories()
    moveAllFiles(unzippedJournal, journalDir)
    moveAllFiles(unzippedSnapshots, snapshotsDir)
    unzipped.delete(true)
  }
}

case object DownloadComplete
