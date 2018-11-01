package mvp2.actors

import mvp2.data.{KeyBlock, MicroBlock, Transaction}

class Accountant extends CommonActor {

  override def specialBehavior: Receive = {
    case microBlock: MicroBlock =>
      if (microBlock.transactions.forall(_.isValid)) updateState(microBlock.transactions)
    case keyBlock: KeyBlock =>
      if (keyBlock.transactions.forall(_.isValid)) updateState(keyBlock.transactions)
  }

  def updateState(transactions: List[Transaction]): Unit = {
    transactions.groupBy(_.publicKey).foreach {
      singleParty =>
        State.updateState(singleParty._1, singleParty._2)
    }
  }
}