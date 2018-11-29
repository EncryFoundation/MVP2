package mvp2.actors

import akka.actor.{ActorRef, Props}
import akka.actor.{ActorSelection, Cancellable}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Epoch
import mvp2.data.InnerMessages._
import mvp2.data.KeyBlock
import mvp2.utils.{EncodingUtils, Settings}
import scala.collection.immutable.SortedMap
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  var nextTurn: Period = Period(KeyBlock(), settings)
  val keyKeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  var myPublicKey: ByteString = ByteString.empty
  var allPublicKeys: Set[ByteString] = Set()
  var nextPeriod: Period = Period(KeyBlock(), settings)
  var lastBlock: KeyBlock = KeyBlock()
  var epoch: Epoch = Epoch(SortedMap())
  var nextEpoch: Option[Epoch] = None
  val heartBeat: Cancellable =
    context.system.scheduler.schedule(10.seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")
  var scheduleForWriting: List[ByteString] = List()
  var hasWritten: Boolean = false

  override def specialBehavior: Receive = {
    case GetNewSyncedEpoch(newEpoch) =>
      epoch = newEpoch
      logger.info(s"Epoch from remote is: $newEpoch. New epoch is: $epoch")
    case SyncingDone =>
      logger.info(s"Synced done on Planner.")
      if (settings.canPublishBlocks)
        context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat milliseconds, self, Tick)
      context.become(syncedNode)
    case keyBlock: KeyBlock => lastBlock = keyBlock
    case PeerPublicKey(key) =>
      allPublicKeys = allPublicKeys + key
      logger.info(s"Set allPublickKeys to1: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
    case MyPublicKey(key) =>
      logger.info(s"Set allPublickKeys to2: ${EncodingUtils.encode2Base16(key)}")
      allPublicKeys = allPublicKeys + key
      myPublicKey = key
      if (settings.otherNodes.isEmpty) self ! SyncingDone
    case _ =>
  }

  def syncedNode: Receive = {
    case keyBlock: KeyBlock =>
      if (!hasWritten && keyBlock.scheduler.nonEmpty) hasWritten = true
      nextPeriod = Period(keyBlock, settings)
      lastBlock = keyBlock
      context.parent ! nextPeriod
    case KeysForSchedule(keys) =>
      logger.info(s"Get peers public keys for schedule: ${keys.map(EncodingUtils.encode2Base16).mkString(",")}")
      allPublicKeys = (keys :+ myPublicKey).sortWith((a, b) => a.utf8String.compareTo(b.utf8String) > 1).toSet
    case MyPublicKey(key) =>
      logger.info("Get key")
      allPublicKeys = allPublicKeys + key
      myPublicKey = key
    case Tick if epoch.isDone =>
      logger.info(s"epoch.isDone. Height of last block is: ${lastBlock.height}")
      hasWritten = false
      epoch = Epoch(lastBlock, allPublicKeys, settings.epochMultiplier)
      logger.info(s"New epoch is: ${epoch.schedule}")
      scheduleForWriting = epoch.schedule.values.toList
      checkMyTurn(isFirstBlock = true, scheduleForWriting)
    case Tick if nextPeriod.timeToPublish =>
      checkMyTurn(isFirstBlock = false, List())
      checkScheduleUpdateTime()
      logger.info("nextPeriod.timeToPublish. Height of last block is: ${lastBlock.height}")
    case Tick if nextPeriod.noBlocksInTime =>
      logger.info("nextPeriod.noBlocksInTime. Height of last block is: ${lastBlock.height}")
      epoch = epoch.noBlockInTime
      if (!hasWritten) checkMyTurn(isFirstBlock = true, scheduleForWriting)
      else checkMyTurn(isFirstBlock = false, List())
      nextPeriod = Period(nextPeriod, settings)
      context.parent ! nextPeriod
      checkScheduleUpdateTime()
    case Tick => logger.info("123")
  }

  def checkMyTurn(isFirstBlock: Boolean, schedule: List[ByteString]): Unit = {
    if (epoch.nextBlock._2 == myPublicKey) publisher ! RequestForNewBlock(isFirstBlock, schedule)
    context.parent ! ExpectedBlockPublicKeyAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
    epoch = epoch.delete
  }

  def checkScheduleUpdateTime(): Unit =
    if (epoch.prepareNextEpoch) {
      networker ! PrepareScheduler
      logger.info("epoch.prepareNextEpoch")
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

  case class Epoch(schedule: SortedMap[Long, ByteString]) {

    def nextBlock: (Long, ByteString) = schedule.head

    def delete: Epoch = this.copy(schedule.tail)

    def delete(height: Long): Epoch = this.copy(schedule.drop(height.toInt))

    def noBlockInTime: Epoch = this.copy(schedule.map(each => (each._1 - 1, each._2)))

    def isDone: Boolean = this.schedule.isEmpty

    def prepareNextEpoch: Boolean = schedule.size <= 2

    override def toString: String = this.schedule.map(epochInfo =>
      s"Height: ${epochInfo._1} -> ${EncodingUtils.encode2Base16(epochInfo._2)}").mkString(",")
  }

  object Epoch extends StrictLogging {
    def apply(lastKeyBlock: KeyBlock, publicKeys: Set[ByteString], multiplier: Int = 1): Epoch = {
      val startingHeight: Long = lastKeyBlock.height + 1
      val numberOfBlocksInEpoch: Int = publicKeys.size * multiplier
      val keysSchedule: List[ByteString] = (1 to multiplier).foldLeft(publicKeys.toList) { case (a, _) => a ::: a }
      val schedule: SortedMap[Long, ByteString] =
        SortedMap((for (i <- startingHeight until startingHeight + numberOfBlocksInEpoch)
          yield i).zip(keysSchedule): _*)
      Epoch(schedule)
    }
  }

  case object Tick

}