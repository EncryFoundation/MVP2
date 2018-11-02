package mvp2.actors

import akka.util.ByteString
import mvp2.utils.Sha256
import mvp2.data.{KeyBlock, MicroBlock, Transaction}
import scala.collection.immutable.HashMap

class Accountant extends CommonActor {

  import Accountant.Account

  var accountsInfo: HashMap[ByteString, Account] = HashMap.empty
  var stateRoot: ByteString = Sha256.toSha256(ByteString(accountsInfo.toString.getBytes))

  override def specialBehavior: Receive = {
    case microBlock: MicroBlock =>
      if (microBlock.transactions.forall(_.isValid)) updateState(microBlock.transactions)
    case keyBlock: KeyBlock =>
      if (keyBlock.transactions.forall(_.isValid)) updateState(keyBlock.transactions)
  }

  def updateState(transactions: List[Transaction]): Unit = {
    transactions.groupBy(_.publicKey).foreach {
      singleParty =>
        var account: Account = accountsInfo.getOrElse(singleParty._1, Account(singleParty._1, List.empty, 0))
        singleParty._2.sortBy(_.nonce).foreach { tx =>
          if (account.nonce + 1 == tx.nonce)
            account = account.copy(data = account.data :+ tx.data, nonce = tx.nonce)
        }
        accountsInfo = accountsInfo + (singleParty._1 -> account)
        stateRoot = Sha256.toSha256(ByteString(accountsInfo.toString.getBytes))
    }
  }
}

object Accountant {

  case class Account(publicKey: ByteString, data: List[ByteString], nonce: Long)
}