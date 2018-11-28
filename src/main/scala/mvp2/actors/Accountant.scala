package mvp2.actors

import akka.util.ByteString
import mvp2.utils.Sha256
import mvp2.data.{KeyBlock, Transaction}
import scala.collection.immutable.HashMap

class Accountant extends CommonActor {

  import Accountant.Account

  var accountsInfo: HashMap[ByteString, Account] = HashMap.empty
  var stateRoot: ByteString = Sha256.toSha256(accountsInfo.toString)

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      logger.info(s"Accountant received keyBlock with height ${keyBlock.height}.")
      if (keyBlock.transactions.forall(_.isValid)) updateState(keyBlock.transactions)
  }

  def updateState(transactions: List[Transaction]): Unit = transactions.groupBy(_.publicKey).foreach {
      singleParty =>
        var account: Account = accountsInfo.getOrElse(singleParty._1, Account(singleParty._1, List.empty, 0))
        singleParty._2.sortBy(_.nonce).foreach { tx =>
          if (account.nonce + 1 == tx.nonce)
            account = account.copy(data = account.data :+ tx.data, nonce = tx.nonce)
        }
        accountsInfo = accountsInfo + (singleParty._1 -> account)
        stateRoot = Sha256.toSha256(accountsInfo.toString)
    }
}

object Accountant {

  case class Account(publicKey: ByteString, data: List[ByteString], nonce: Long)

}