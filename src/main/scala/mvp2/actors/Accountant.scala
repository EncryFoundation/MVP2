package mvp2.actors

import akka.typed.ActorRef
import akka.util.ByteString
import mvp2.utils.Sha256
import mvp2.data.{KeyBlock, Transaction}

import scala.collection.immutable.HashMap
import akka.typed.scaladsl.adapter._
import mvp2.actors.TypeAccountant.{BlocksList, TxsList}

class Accountant extends CommonActor {

  import Accountant.Account

  var accountsInfo: HashMap[ByteString, Account] = HashMap.empty
  var stateRoot: ByteString = Sha256.toSha256(accountsInfo.toString)

  val typedActor: ActorRef[TypeAccountant.Commands] = context.spawn(TypeAccountant.behavior, "typedActor")

  typedActor ! TxsList(List(Transaction(), Transaction()), self)
  typedActor ! BlocksList(List(KeyBlock(), KeyBlock()), self)

  val otherTypeActor: ActorRef[List[Transaction]] = context.spawn(TypeAccountant.overBehavior, "otherTypeAccount")

  otherTypeActor ! List(Transaction(), Transaction())
  otherTypeActor ! List(KeyBlock(), KeyBlock())

  override def specialBehavior: Receive = {

    case TypeAccountant.TxsList =>
    case TypeAccountant.BlocksList =>

    case keyBlock: KeyBlock =>
      println(s"Accountant received keyBlock with height ${keyBlock.height}.")
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