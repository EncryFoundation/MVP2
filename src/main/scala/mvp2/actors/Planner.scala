package mvp2.actors

import akka.actor.{ActorSelection, Cancellable}
import akka.util.ByteString
import mvp2.actors.Planner.Epoch
import mvp2.data.InnerMessages.{ExpectedBlockSignatureAndHeight, Get, MyPublicKey, PeerPublicKey}
import mvp2.data.KeyBlock
import mvp2.utils.{EncodingUtils, Settings}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  var myPublicKey: ByteString = ByteString.empty
  var allPublicKeys: Set[ByteString] = Set()
  var nextPeriod: Period = Period(KeyBlock(), settings)
  var lastBlock: KeyBlock = KeyBlock()
  var epoch: Epoch = Epoch(Map())
  val heartBeat: Cancellable =
    context.system.scheduler.schedule(10.seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      nextPeriod = Period(keyBlock, settings)
      lastBlock = keyBlock
      context.parent ! nextPeriod
      println(s"Got new keyBlock with height ${keyBlock.height} on a Planner and ${keyBlock.timestamp}")
      println(s"Got new period on a Planner${nextPeriod.begin} ${nextPeriod.end}")
    case PeerPublicKey(key) =>
//      println(s"Set before new peer's Key: $allPublicKeys and key is $key")
      allPublicKeys = allPublicKeys + key
//      println(s"Got public key from remote: ${EncodingUtils.encode2Base16(key)} on Planner. " +
//        s"New key's collection is $allPublicKeys")
    case MyPublicKey(key) =>
      allPublicKeys = allPublicKeys + key
      myPublicKey = key
      //println(s"Got my public key: ${EncodingUtils.encode2Base16(key)} on Planner.")
    case Tick if epoch.isDone =>
      //println(s"Epoch is done. Last key block is: $lastBlock. Keys set is: $allPublicKeys. Is it empty ${allPublicKeys.isEmpty}")
      epoch = Epoch(lastBlock, allPublicKeys)
      println(s"Last epoch has done. Create new epoch - ${epoch.schedule}")
      if (epoch.nextBlock._2 == myPublicKey) {
        publisher ! Get
        context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
      } else context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
      epoch = epoch.delete
      println(s"Epoch after creating new and  removing last element is: ${epoch.schedule}")
    case Tick if nextPeriod.timeToPublish =>
      println(s"It's time to publish new block!")
      if (epoch.nextBlock._2 == myPublicKey) {
        publisher ! Get
        context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
      }
      else context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
      epoch = epoch.delete
      println(s"Epoch after publishing and after removing last element is: ${epoch.schedule}")
    case Tick if nextPeriod.noBlocksInTime =>
      //println(s"No blocks in time. Planner added ${newPeriod.exactTime - System.currentTimeMillis} milliseconds.")
      //println(s"${epoch.schedule} before")
      val newEpoch: Epoch = epoch.noBlockInTime
      println(s"no block in time. Epoch with noBlocks is: ${newEpoch.schedule}")
      if (newEpoch.isDone) {
        println(s"no blocks in time Epoch is done. Send self new tick with isDone epoch.")
        epoch = newEpoch
      } else {
        epoch = newEpoch
        if (epoch.nextBlock._2 == myPublicKey) {
          publisher ! Get
          context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
        }
        else context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
        epoch = epoch.delete
        println(s"no blocks in time Epoch ${epoch.schedule} after no blocks in time")
      }
      val newPeriod = Period(nextPeriod, settings)
      nextPeriod = newPeriod
      context.parent ! nextPeriod
      //println(s"Epoch after noBlockInTime and after removing last element is: ${epoch.schedule}")
    case Tick =>
  }
}

object Planner {

  case class Period(begin: Long, exactTime: Long, end: Long) {
    def timeToPublish: Boolean = {
      val now: Long = System.currentTimeMillis
      now >= this.begin && now <= this.end
    }

    def noBlocksInTime: Boolean = System.currentTimeMillis > this.end

  }

  object Period {

    def apply(lastKeyBlock: KeyBlock, settings: Settings): Period = {
      val exactTimestamp: Long = lastKeyBlock.timestamp + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }

    def apply(previousPeriod: Period, settings: Settings): Period = {
      val exactTimestamp: Long = previousPeriod.exactTime + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }
  }

  case class Epoch(schedule: Map[Long, ByteString]) {

    def nextBlock: (Long, ByteString) = schedule.head

    def delete: Epoch = this.copy(schedule - schedule.head._1)

    def delete(height: Long): Epoch = this.copy(schedule = schedule.drop(height.toInt))

    def noBlockInTime: Epoch = this.copy(schedule.map(each => (each._1 - 1, each._2)))

    def isDone: Boolean = this.schedule.isEmpty
  }

  object Epoch {
    def apply(lastKeyBlock: KeyBlock, publicKeys: Set[ByteString], multiplier: Int = 1): Epoch = {
      val startingHeight: Long = lastKeyBlock.height + 1
      val numberOfBlocksInEpoch: Int = publicKeys.size * multiplier
      val schedule: Map[Long, ByteString] =
        (for (i <- startingHeight until startingHeight + numberOfBlocksInEpoch)
          yield i).zip(publicKeys).toMap[Long, ByteString]
      Epoch(schedule)
    }
  }

  case object Tick
}