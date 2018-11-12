package mvp2.actors

import java.util.UUID.randomUUID
import akka.util.ByteString
import com.google.common.io.BaseEncoding
import io.circe.Json
import mvp2.http.{EthResponse, EthereumService}
import mvp2.utils.{EthRequestType, EthereumSettings}
import scala.concurrent.duration._

class Anchorer(ethereumSettings: EthereumSettings) extends CommonActor {

  lazy val gasAmount = 30000
  lazy val ethToTransfer = 0.01
  private var lastEthBlockHash: String = _
  private var unconfirmedQueue: List[UnconfirmedTransaction] = _

  override def specialBehavior: Receive = {
    case blockHash: ByteString =>
      unconfirmedQueue = UnconfirmedTransaction(randomUUID().toString, blockHash, System.currentTimeMillis() / 1000L,
        "", isUnlocked = false) :: unconfirmedQueue
      sendUnlockAccount(unconfirmedQueue.head)
      context.system.scheduler.scheduleOnce(30 minutes)(retryUnconfirmed())
    case response: EthResponse => response.rtype match {
      case EthRequestType.UNLOCKACC => if (getUnlockResult(response.responseBody))
        unconfirmedQueue = unconfirmedQueue.filter(_.innerId == response.innerId).head.copy(isUnlocked = true) ::
          unconfirmedQueue.filter(_.innerId != response.innerId)
        sendEthereumTransaction(unconfirmedQueue.head)
      case EthRequestType.SENDTX =>
        unconfirmedQueue = unconfirmedQueue.filter(_.innerId == response.innerId)
          .head.copy(transactionEthID = getTransactionId(response.responseBody)) ::
          unconfirmedQueue.filter(_.innerId != response.innerId)
        sendTransactionReceiptRequest(unconfirmedQueue.head)
      case EthRequestType.GETRESULT => if (getTransactionReceipt(response.responseBody)._1) {
        lastEthBlockHash = getTransactionReceipt(response.responseBody)._2
        logger.info("transaction with block hash: " + ByteString.toString +
          "written in Ethereum block: " + lastEthBlockHash)
        unconfirmedQueue = unconfirmedQueue.filter(_.innerId != response.innerId)
      }
    }
  }

  def retryUnconfirmed() : Unit = {
    unconfirmedQueue = unconfirmedQueue.sortBy(_.timeStamp)
    unconfirmedQueue.foreach(e => sendUnlockAccount(e))
  }

  def sendEthereumTransaction(transaction: UnconfirmedTransaction): Unit = {
    val transactionJson: Json = Json.fromFields(List(
      ("jsonrpc", Json.fromDoubleOrNull(2.0)),
      ("method", Json.fromString("eth_sendTransaction")),
      ("params", Json.fromFields(List(
        ("from", Json.fromString(ethereumSettings.userAccount)),
        ("to", Json.fromString(ethereumSettings.receiverAccount)),
        ("value", Json.fromDoubleOrNull(ethToTransfer)),
        ("gas", Json.fromString(Integer.toHexString(gasAmount))),
        ("gasPrice", Json.fromString(Integer.toHexString(ethereumSettings.gasPrice))),
        ("data", Json.fromString(encode2Base16(transaction.blockHash)))
      ))),
      ("id", Json.fromInt(1))
    ))
    EthereumService
      .sendRequestToEthereum(transaction.innerId, transactionJson, ethereumSettings.peerRPCAddress, EthRequestType.SENDTX)
  }

  def sendUnlockAccount(transaction: UnconfirmedTransaction): Unit = {
    val jsonToUnlock = Json.fromFields(List(
      ("jsonrpc", Json.fromDoubleOrNull(2.0)),
      ("method", Json.fromString("personal_unlockAccount")),
      ("params", Json.fromValues(List(
        Json.fromString(ethereumSettings.userAccount),
        Json.fromString(ethereumSettings.userPassword),
        Json.fromInt(600)
      ))),
      ("id", Json.fromInt(67))
    ))
    EthereumService
      .sendRequestToEthereum(transaction.innerId, jsonToUnlock, ethereumSettings.peerRPCAddress, EthRequestType.UNLOCKACC)
  }

  def sendTransactionReceiptRequest(transaction: UnconfirmedTransaction): Unit = {
    val requestBody = Json.fromFields(List(
      ("jsonrpc", Json.fromDoubleOrNull(2.0)),
      ("method", Json.fromString("eth_getTransactionReceipt")),
      ("params", Json.fromValues(List(Json.fromString(transaction.transactionEthID)))),
      ("id", Json.fromInt(1))
    ))
    EthereumService
      .sendRequestToEthereum(transaction.innerId, requestBody, ethereumSettings.peerRPCAddress, EthRequestType.GETRESULT)
  }

  def getUnlockResult(json: Json): Boolean = json.hcursor.downField("result").as[Boolean].getOrElse(false)

  def getTransactionReceipt(json: Json): (Boolean, String) =
    (json.hcursor.downField("status").as[String].getOrElse("") == "0x1",
      json.hcursor.downField("blockHash").as[String].getOrElse(""))

  def getTransactionId(json: Json): String = json.hcursor.downField("result").as[String].getOrElse("")

  def getLastEthBlockHash: String = lastEthBlockHash

  def encode2Base16(bytes: ByteString): String = "0x" + BaseEncoding.base16().encode(bytes.toArray)

}

case class UnconfirmedTransaction(innerId: String, blockHash: ByteString, timeStamp: Long,
                                  transactionEthID: String, isUnlocked: Boolean)