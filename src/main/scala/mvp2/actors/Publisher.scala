package mvp2.actors

import mvp2.data.{Block, Transaction}

class Publisher extends CommonActor {

  var mempool: List[Transaction] = List.empty

  override def specialBehavior: Receive = {
    case transaction: Transaction if transaction.isValid => transaction :: mempool
    case block: Block =>
  }

}
