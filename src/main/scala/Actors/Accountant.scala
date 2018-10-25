package Actors

import Data.{GeneralBlock, MicroBlock, Transaction}
import akka.util.ByteString
import scala.collection.immutable.HashMap

class Accountant extends CommonActor {

  var currentState: State = GenesisState(HashMap.empty)

  override def specialBehavior: Receive = {
    case microBlock: MicroBlock =>
      if (microBlock.transactions.forall(_.isValid)) updateState(microBlock.transactions)
    case keyBlock: GeneralBlock =>
      if (keyBlock.transactions.forall(_.isValid)) updateState(keyBlock.transactions)
  }

  def updateState(transactions: List[Transaction]): Unit = {
    transactions.groupBy(_.publicKey).foreach {
      singleParty =>
        currentState match {
          case fs: FunctioningState => updateState(singleParty._1, fs.accountsInfo, singleParty._2)
          case gs: GenesisState => updateState(singleParty._1, gs.accountsInfo, singleParty._2)
        }
    }
  }

  def updateState(pubKey: ByteString, accounts: HashMap[ByteString, Account], transactions: List[Transaction]): Unit = {
    var account: Account = accounts.getOrElse(pubKey, Account(pubKey, List.empty,0))
    transactions.sortBy(_.nonce).foreach { tx =>
      if (account.nonce + 1 == tx.nonce)
        account = account.copy(data = account.data :+ tx.data.getOrElse(ByteString.empty), nonce = tx.nonce)
    }
    currentState = FunctioningState(accounts + (pubKey -> account))
  }
}