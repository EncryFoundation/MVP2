package mvp2.actors

import java.util.concurrent.TimeUnit
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import io.circe.Json
import mvp2.utils.EthereumSettings
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Anchorer(ethereumSettings: EthereumSettings) extends CommonActor {

  lazy val gasAmount = 30000
  lazy val ethToTransfer = 0.01
  private var lastEthBlockHash: String = _
  private var unconfirmedQueue: List[UnconfirmedTransaction] = _

  override def specialBehavior: Receive = {
    case blockHash: ByteString =>
      if (unlockAccount) {
        val transactionID: String = sendEthereumTransaction(blockHash)
        unconfirmedQueue = UnconfirmedTransaction(blockHash, transactionID, inChain = false) :: unconfirmedQueue
        getEthBlockHashWithTransaction(transactionID) match {
          case Some(value) => lastEthBlockHash = value
            unconfirmedQueue.filter(_.transactionID!=transactionID)
          case None => logger.warn("Still not in the block: " +
            unconfirmedQueue.filter(_.transactionID==transactionID).head.toString)
        }
      }
  }

  def sendEthereumTransaction(blockHash: ByteString) = {
    val transaction: Json = Json.fromFields(List(
      ("jsonrpc", Json.fromDoubleOrNull(2.0)),
      ("method", Json.fromString("eth_sendTransaction")),
      ("params", Json.fromFields(List(
        ("from", Json.fromString(ethereumSettings.userAccount)),
        ("to", Json.fromString(ethereumSettings.receiverAccount)),
        ("value", Json.fromDoubleOrNull(ethToTransfer)),
        ("gas", Json.fromString(Integer.toHexString(gasAmount))),
        ("gasPrice", Json.fromString(Integer.toHexString(ethereumSettings.gasPrice))),
        ("data", Json.fromString(blockHash.toList.map("%02X" format _).mkString.toList.map(_.toInt.toHexString).mkString))
      ))),
      ("id", Json.fromInt(1))
    ))

    "tx_id"
  }

  def unlockAccount: Boolean = {
    val jsonToUnlock = Json.fromFields(List (
      ("jsonrpc", Json.fromDoubleOrNull(2.0)),
      ("method", Json.fromString("personal_unlockAccount")),
      ("params", Json.fromValues(List(
        Json.fromString(ethereumSettings.userAccount),
        Json.fromString(ethereumSettings.userPassword),
        Json.fromInt(600)
      )))
      ("id", Json.fromInt(67))
    ))
//    val unlockResponseFuture: Future[HttpResponse] = Http().singleRequest(
//      HttpRequest(
//        HttpMethods.POST, ethereumSettings.peerRPCAddress,
//        entity=HttpEntity(MediaTypes.`application/json`, jsonToUnlock.toString)
//      ))
//    unlockResponseFuture.onComplete {
//      case Success(res) =>
//    }
  }

  def getEthBlockHashWithTransaction(transactionID: String): Option[String] = {
    val requestBody = Json.fromFields(List (
      ("jsonrpc", Json.fromDoubleOrNull(2.0)),
      ("method", Json.fromString("eth_getTransactionReceipt")),
      ("params", Json.fromValues(List(Json.fromString(transactionID))))
      ("id", Json.fromInt(1))
    ))
    Some("block_hash")
  }

  def getLastEthBlockHash: String = lastEthBlockHash

  def sendRequestReceiveResponse(json: Json) = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(
      HttpRequest(
        HttpMethods.POST, ethereumSettings.peerRPCAddress,
        entity=HttpEntity(MediaTypes.`application/json`, json.toString)
      ))
    responseFuture.onComplete {
      case Success(response) => response.status match {
        case StatusCodes.OK if (response.entity.contentType == ContentTypes.`application/json`) =>
          Unmarshal(response.entity).to[String].map()
      }
      case Failure(e) => logger.error(e.toString)
    }
    Await.result(responseFuture, Duration.apply(30, TimeUnit.SECONDS))
  }
}

case class UnconfirmedTransaction(BlockHash: ByteString, transactionID: String, inChain:Boolean)
