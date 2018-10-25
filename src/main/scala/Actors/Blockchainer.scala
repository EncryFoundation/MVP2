package Actors

import Data.{Blockchain, GeneralBlock, MicroBlock}
import akka.persistence.{PersistentActor, RecoveryCompleted}

class Blockchainer extends PersistentActor {

  var blockchain: Blockchain

  override def receiveRecover: Receive = {
    case microBlock: MicroBlock => updateHeaders(header)
    case generalBlock:GeneralBlock =>
    case RecoveryCompleted => context.system.scheduler.scheduleOnce(5 seconds)(self ! CheckAllBlocksSent)
  }

  override def receiveCommand: Receive = {
    case CheckAllBlocksSent =>
      if (completedBlocks.isEmpty) notifyRecoveryCompleted()
      else context.system.scheduler.scheduleOnce(5 seconds)(self ! CheckAllBlocksSent)
    case SendBlocks =>
      val blocksToSend: Seq[Block] = completedBlocks.take(settings.levelDb
        .map(_.batchSize)
        .getOrElse(throw new RuntimeException("batchsize not specified"))).values.toSeq

  }

  def updateHeaders(header: Header): Unit = {
    val prevValue: (Header, Int) = headers.getOrElse(Algos.encode(header.id), (header, -1))
    headers += Algos.encode(header.id) -> (prevValue._1, prevValue._2 + 1)
    if (!nonCompletedBlocks.contains(Algos.encode(header.payloadId)))
      nonCompletedBlocks += Algos.encode(header.payloadId) -> Algos.encode(header.id)
    else {
      nonCompletedBlocks = (nonCompletedBlocks - Algos.encode(header.payloadId)) +
        (Algos.encode(header.payloadId) -> Algos.encode(header.id))
      createBlockIfPossible(header.payloadId)
    }
  }
}


