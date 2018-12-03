package mvp2.actors

import java.text.SimpleDateFormat

import akka.actor.{ActorRef, Props}
import akka.actor.{ActorSelection, Cancellable}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.actors.Planner.Epoch
import mvp2.data.InnerMessages._
import mvp2.data.KeyBlock
import mvp2.utils.{EncodingUtils, Settings}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  val keyKeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  var myPublicKey: ByteString = ByteString.empty
  var allPublicKeys: List[ByteString] = List.empty
  var nextPeriod: Period = Period(KeyBlock(), settings)
  var lastBlock: KeyBlock = KeyBlock()
  var epoch: Epoch = Epoch(List.empty)
  val heartBeat: Cancellable =
    context.system.scheduler.schedule(10.seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")
  var scheduleForWriting: List[ByteString] = List()
  var hasWritten: Boolean = false
  var needToCheckTimeToPublish: Boolean = true
  var isRemoved: Boolean = false

  override def specialBehavior: Receive = {
    case SyncingDone =>
      logger.info(s"Synced done on Planner.")
      if (settings.canPublishBlocks)
        context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat milliseconds, self, Tick)
      context.become(syncedNode)
    case keyBlock: KeyBlock =>
      lastBlock = keyBlock
      nextPeriod = Period(keyBlock, settings)
      logger.info(s"Epoch before trying to update with new block from network is: $epoch")
      if (keyBlock.scheduler.nonEmpty) epoch = Epoch(keyBlock.scheduler)
      epoch = epoch.dropNextPublisherPublicKey
      logger.info(s"Current epoch is(before sync): $epoch. Height of last block is: ${lastBlock.height}")
    case PeerPublicKey(key) =>
      allPublicKeys = (allPublicKeys :+ key).sortWith((a, b) => a.utf8String.compareTo(b.utf8String) > 1)
      logger.info(s"Set allPublickKeys to1 : ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
    case MyPublicKey(key) =>
      logger.info(s"Set allPublickKeys to2 : ${EncodingUtils.encode2Base16(key)}")
      allPublicKeys = (allPublicKeys :+ key).sortWith((a, b) => a.utf8String.compareTo(b.utf8String) > 1)
      myPublicKey = key
      if (settings.otherNodes.isEmpty) self ! SyncingDone
    case _ =>
  }

  def syncedNode: Receive = {
    case keyBlock: KeyBlock =>
      nextPeriod = Period(keyBlock, settings)
      needToCheckTimeToPublish = true
      lastBlock = keyBlock
      if (!isRemoved) epoch = epoch.dropNextPublisherPublicKey
      if (lastBlock.scheduler.nonEmpty) hasWritten = true
      logger.info(s"Last block was updated. Height of last block is: ${lastBlock.height}. Period was updated. " +
        s"New period is: $nextPeriod.")
      context.parent ! nextPeriod
    case KeysForSchedule(keys) =>
      logger.info(s"Get peers public keys for schedule: ${keys.map(EncodingUtils.encode2Base16).mkString(",")}")
      allPublicKeys = (keys :+ myPublicKey).sortWith((a, b) => a.utf8String.compareTo(b.utf8String) > 1)
      logger.info(s"Current epoch is: $epoch. Height of last block is: ${lastBlock.height}")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
    case MyPublicKey(key) =>
      logger.info("Get my key")
      allPublicKeys = (allPublicKeys :+ key).sortWith((a, b) => a.utf8String.compareTo(b.utf8String) > 1)
      logger.info(s"Current epoch is: $epoch. Height of last block is: ${lastBlock.height}")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      myPublicKey = key
    case Tick if epoch.isDone =>
      hasWritten = false
      logger.info(s"epoch.isDone. Height of last block is: ${lastBlock.height}")
      logger.info(s"Current epoch is: $epoch. Height of last block is: ${lastBlock.height}")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      epoch = Epoch(allPublicKeys, settings.epochMultiplier)
      logger.info(s"New epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      scheduleForWriting = epoch.schedule
      checkMyTurn(scheduleForWriting)
      isRemoved = true
    case Tick if nextPeriod.timeToPublish && needToCheckTimeToPublish =>
      logger.info(s"nextPeriod.timeToPublish. Height of last block is: ${lastBlock.height}")
      checkMyTurn(scheduleForWriting)
      needToCheckTimeToPublish = false
      isRemoved = true
      logger.info(s"Current epoch is: $epoch. Height of last block is: ${lastBlock.height}")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      checkScheduleUpdateTime()
    case Tick if nextPeriod.noBlocksInTime =>
      logger.info(s"nextPeriod.noBlocksInTime. Height of last block is: ${lastBlock.height}")
      //epoch = epoch.dropNextPublisherPublicKey
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      nextPeriod = Period(nextPeriod, settings)
      if (!hasWritten) epoch = Epoch(epoch.schedule)
      checkMyTurn(scheduleForWriting)
      context.parent ! nextPeriod
      needToCheckTimeToPublish = true
      checkScheduleUpdateTime()
      isRemoved = true
    case Tick =>
      logger.info("123")
    //      logger.info(s"Current epoch is: $epoch. Height of last block is: ${lastBlock.height}")
    //      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
  }

  def checkMyTurn(schedule: List[ByteString]): Unit = {
    logger.info(s"Going to check publisher at height: ${lastBlock.height + 1}." +
      s" Next publisher is: ${EncodingUtils.encode2Base16(epoch.publicKeyOfNextPublisher)}. " +
      s"My key: ${EncodingUtils.encode2Base16(myPublicKey)}. Result: ${epoch.publicKeyOfNextPublisher == myPublicKey}." +
      s" ${epoch.publicKeyOfNextPublisher == myPublicKey} ... ${schedule.map(EncodingUtils.encode2Base16).mkString(",")}")
    if (epoch.publicKeyOfNextPublisher == myPublicKey) {
      logger.info(s"Got request for a new local block. Write schedule inside is. ${epoch.full}. Schedule is: " +
        s"${schedule.map(EncodingUtils.encode2Base16).mkString(",")}")
      publisher ! RequestForNewBlock(epoch.full, schedule)
    }
    logger.info(s"${epoch.full} && ${lastBlock.height}")
    context.parent ! ExpectedBlockPublicKeyAndHeight(epoch.publicKeyOfNextPublisher)
    epoch = epoch.dropNextPublisherPublicKey
    logger.info(s"Epoch after checkMyTurn is: $epoch")
  }

  def checkScheduleUpdateTime(): Unit =
    if (epoch.prepareNextEpoch) {
      networker ! PrepareScheduler
      logger.info("epoch.prepareNextEpoch")
    }
}

object Planner {

  case class Period(begin: Long, exactTime: Long, end: Long) {

    val df = new SimpleDateFormat("HH:mm:ss")

    def timeToPublish: Boolean = {
      val now: Long = System.currentTimeMillis
      now >= this.begin && now <= this.end
    }

    def noBlocksInTime: Boolean = System.currentTimeMillis > this.end

    override def toString: String = s"Period(begin: ${df.format(begin)}. " +
      s"ExactTime: ${df.format(exactTime)}. end: ${df.format(end)})"
  }

  object Period extends StrictLogging {

    def apply(lastKeyBlock: KeyBlock, settings: Settings): Period = {
      val exactTimestamp: Long = lastKeyBlock.timestamp + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }

    def apply(previousPeriod: Period, settings: Settings): Period = {
      val exactTimestamp: Long = previousPeriod.exactTime + settings.blockPeriod
      logger.info(s"Generating next period after $previousPeriod")
      val newPeriod: Period =
        Period(
          exactTimestamp - settings.biasForBlockPeriod,
          exactTimestamp,
          exactTimestamp + settings.biasForBlockPeriod
        )
      logger.info(s"New period: $newPeriod")
      newPeriod
    }
  }

  case class Epoch(schedule: List[ByteString], full: Boolean) {

    def isApplicableBlock(block: KeyBlock): Boolean = block.publicKey == schedule.head

    def publicKeyOfNextPublisher: ByteString = schedule.head

    def dropNextPublisherPublicKey: Epoch = if (schedule.nonEmpty) this.copy(schedule.tail, full = false) else this

    def isDone: Boolean = this.schedule.isEmpty

    def prepareNextEpoch: Boolean = schedule.size <= 2

    override def toString: String = this.schedule.map(EncodingUtils.encode2Base16).mkString(",")
  }

  object Epoch extends StrictLogging {

    def apply(publicKeys: List[ByteString], multiplier: Int = 1): Epoch =
      Epoch((1 to multiplier).foldLeft(List[ByteString]()) { case (a, _) => a ::: publicKeys }, full = true)

    def apply(schedule: List[ByteString]): Epoch = Epoch(schedule, full = true)
  }

  case object Tick

}