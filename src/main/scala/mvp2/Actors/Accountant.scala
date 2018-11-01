package mvp2.Actors

import mvp2.Data.{GeneralBlock, MicroBlock, Transaction}
import mvp2.Messages.CurrentAccountantInfo
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class Accountant extends CommonActor {

  override def preStart(): Unit = {
    logger.info("Starting the Accountant!")
    context.system.scheduler.schedule(1.seconds, 10.seconds)(sendInformation)
  }
  override def specialBehavior: Receive = {
    case microBlock: MicroBlock =>
      if (microBlock.transactions.forall(_.isValid)) updateState(microBlock.transactions)
    case keyBlock: GeneralBlock =>
      if (keyBlock.transactions.forall(_.isValid)) updateState(keyBlock.transactions)
  }

  def updateState(transactions: List[Transaction]): Unit = {
    transactions.groupBy(_.publicKey).foreach {
      singleParty =>
        State.updateState(singleParty._1, singleParty._2)
    }
  }

  def sendInformation: Unit =
    context.actorSelection("/user/starter/informator") !
      CurrentAccountantInfo(State.getAccountsInfo)

}

