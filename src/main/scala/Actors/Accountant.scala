package Actors

import Data.{GeneralBlock, MicroBlock, Transaction}
import akka.util.ByteString

import scala.collection.immutable.HashSet

class Accountant extends CommonActor {

  var currentState: State = GenesisState(HashSet.empty)

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

  def updateState(pubKey: ByteString, accounts: HashSet[Account], transactions: List[Transaction]): Unit = {
    var account: Account = accounts.filter(_.publicKey.equals(pubKey)).head
    transactions.sortBy(_.nonce).foreach { tx =>
      if (account.nonce + 1 == tx.nonce)
        account = account.copy(data = account.data :+ tx.data.getOrElse(ByteString.empty), nonce = tx.nonce)
    }
    currentState = FunctioningState(accounts.map(acc => if (acc.publicKey.equals(pubKey)) account else acc))
  }
}