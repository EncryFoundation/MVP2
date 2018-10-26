package mvp2.Actors

import akka.util.ByteString
import mvp2.Data.Transaction
import utils.Sha256
import scala.collection.immutable.HashMap

object State {
  private var accountsInfo: HashMap[ByteString, Account] = HashMap.empty
  private var stateRoot: ByteString = Sha256.toSha256(accountsInfo.toString)

  def updateState(pubKey: ByteString, transactions: List[Transaction]): Unit = {
    var account: Account = accountsInfo.getOrElse(pubKey, Account(pubKey, List.empty, 0))
    transactions.sortBy(_.nonce).foreach { tx =>
      if (account.nonce + 1 == tx.nonce)
        account = account.copy(data = account.data :+ tx.data.getOrElse(ByteString.empty), nonce = tx.nonce)
    }
    accountsInfo = accountsInfo + (pubKey -> account)
    stateRoot = Sha256.toSha256(accountsInfo.toString)
  }

  def getStateRoot: ByteString = stateRoot

  def getAccountsInfo: HashMap[ByteString, Account] = accountsInfo
}

case class Account(publicKey: ByteString, data: List[ByteString], nonce: Long)