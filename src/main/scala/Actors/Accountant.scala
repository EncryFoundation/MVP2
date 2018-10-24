package Actors

import Data.{GeneralBlock, MicroBlock, Transaction}
import akka.util.ByteString

import scala.collection.immutable.HashSet

class Accountant extends CommonActor {

  var currentState: State = GenesisState(HashSet.empty)

  override def specialBehavior: Receive = {
    case microBlock: MicroBlock => {
      if (microBlock.transactions.forall(_.isValid)) updateState(microBlock.transactions)
    }
    case keyBlock: GeneralBlock =>
  }

  def updateState(transactions: List[Transaction]): Unit={
    transactions.groupBy(_.publicKey).foreach{
      singleParty => currentState match {
        case GenesisState(accounts) => updateAccount(singleParty._1, accounts, singleParty._2)
        case FunctioningState(accounts) => updateAccount(singleParty._1, accounts, singleParty._2)
      }
    }
  }

  def updateAccount(pubKey: ByteString, accounts: HashSet[Account], transactions: List[Transaction]): Unit = {

  }
}