package mvp2.actors

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import akka.util.ByteString
import mvp2.data.{Block, KeyBlock, Transaction}
import mvp2.utils.Sha256
import scala.collection.immutable.HashMap

object TypedAccountant {

  sealed trait TypedCommands
  final case class MessageToReply(response: String)
  final case class TransactionsTyped(transactions: List[Transaction],
                                     replyTo: ActorRef[MessageToReply]) extends TypedCommands
  final case class BlocksTyped(blocks: List[Block], replyTo: ActorRef[MessageToReply]) extends TypedCommands
  final case class TransactionTyped(transaction: Transaction, replyTo: ActorRef[MessageToReply]) extends TypedCommands
  final case class BlockTyped(keyBlock: KeyBlock, replyTo: ActorRef[MessageToReply]) extends TypedCommands

  case class Account(publicKey: ByteString, data: List[ByteString], nonce: Long)

  val behavior: Behavior[TypedCommands] = typeAcc()

  private def typeAcc(accountsInfo: HashMap[ByteString, Account] = HashMap.empty,
                      stateRoot: ByteString = ByteString.empty): Behavior[TypedCommands] =
    Actor.immutable[TypedCommands] { (ctx, msg) =>
      msg match {
        case TransactionsTyped(txs, _) => typeAcc()
        case BlocksTyped(blocks, _) => typeAcc()
        case TransactionTyped(transaction, _) => typeAcc()
        case BlockTyped(block, _) =>
          println("Got new block on typed accountant.")
          if (block.transactions.forall(_.isValid)) {
            println("All transactions are valid. Starting update state.")
            val newState: HashMap[ByteString, Account] = updateState(block.transactions, accountsInfo)
            val newStateRoot = Sha256.toSha256(newState.toString)
            typeAcc(newState, newStateRoot)
          } else typeAcc()
      }
    }

  def updateState(transactions: List[Transaction],
                  info: HashMap[ByteString, Account]): HashMap[ByteString, Account] = {
    transactions
      .groupBy(_.publicKey)
      .foldLeft(info) { case (state, pair) =>
        val account: Account = state.getOrElse(pair._1, Account(pair._1, List(), 0))
        val accountNew = pair._2.sortBy(_.nonce).foldLeft(account) { case (acc, transaction) =>
          if (acc.nonce + 1 == transaction.nonce)
            Account(acc.publicKey, acc.data :+ transaction.data, transaction.nonce)
          else acc
        }
        info + (pair._1 -> accountNew)
      }
  }
}