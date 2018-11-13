package mvp2.actors

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import akka.util.ByteString
import mvp2.actors.Accountant.Account
import mvp2.data.{Block, Transaction}

import scala.collection.immutable.HashMap

object TypeAccountant {

  sealed trait Commands

  final case class MessageToReply(args: Any*) extends Commands

  final case class TxsList(txs: List[Transaction], replyTo: ActorRef[MessageToReply]) extends Commands

  final case class BlocksList(blocks: List[Block], replyTo: ActorRef[MessageToReply]) extends Commands

  final case class RequestReply(replyTo: ActorRef[MessageToReply]) extends Commands

  val behavior: Behavior[Commands] = typeAcc()

  private def typeAcc(accountsInfo: HashMap[ByteString, Account] = HashMap.empty,
                      stateRoot: ByteString = ByteString.empty): Behavior[Commands] =
    Actor.immutable[Commands] { (ctx, msg) =>
      msg match {
        case RequestReply(whom) =>
          whom ! MessageToReply(1, 2, 3)
          typeAcc()
        case TxsList(txs, whom) =>
          println(txs + " txs")
          whom ! MessageToReply("asdfg")
          typeAcc()
        case BlocksList(blocks, whom) =>
          println(blocks + " blocks")
          whom ! MessageToReply("qwkrpwofjodj")
          typeAcc()
      }
    }

  val overBehavior: Behavior[List[Transaction]] = otherTypeAcc()

  private def otherTypeAcc(): Behavior[List[Transaction]] =
    Actor.immutable[List[Transaction]] { (ctx, msg) =>
      msg match {
        case list: List[Transaction] =>
          println(list + " tx")
          otherTypeAcc()
        case list: List[Block] =>
          println(list + " blocks")
          otherTypeAcc()
      }
    }
}