package mvp2.actors

import akka.actor.ActorRefFactory
import mvp2.messages.{CurrentBlockchainInfo, Get, GetLightChain}
import mvp2.http.Routes
import akka.http.scaladsl.Http
import akka.util.ByteString
import mvp2.MVP2._
import mvp2.actors.Informator.LightKeyBlock
import mvp2.utils.Settings

class Informator(settings: Settings) extends CommonActor {

  Informator.start(settings, context)

  var actualInfo: CurrentBlockchainInfo = CurrentBlockchainInfo()
  var lightChain: List[LightKeyBlock] = List()

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case GetLightChain => sender ! lightChain
    case Get => sender ! actualInfo
    case currentBlockchainInfo: CurrentBlockchainInfo =>
      lightChain = currentBlockchainInfo.lastKeyBlock.map(block => LightKeyBlock(
        block.height,
        block.timestamp,
        block.previousKeyBlockHash,
        block.currentBlockHash,
        block.transactions.size,
        block.data
      )).getOrElse(LightKeyBlock()) :: lightChain
      actualInfo = currentBlockchainInfo
  }
}

object Informator {

  def start(settings: Settings, context: ActorRefFactory): Unit = Http().bindAndHandle(
    Routes(settings, context).route,
    settings.apiSettings.httpHost,
    settings.apiSettings.httpPort
  )

  case class LightKeyBlock(height: Long = 0,
                           timestamp: Long = 0,
                           prevBlockHash: ByteString = ByteString.empty,
                           currentBlockHash: ByteString = ByteString.empty,
                           txsNum: Int = 0,
                           data: ByteString = ByteString.empty)

}