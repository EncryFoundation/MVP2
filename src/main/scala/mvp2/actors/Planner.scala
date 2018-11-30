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
  var epoch: Epoch = Epoch(List.empty)
  val heartBeat: Cancellable =
    context.system.scheduler.schedule(10.seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")
  val networker: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/networker")
  var scheduleForWriting: List[ByteString] = List()
  var hasWritten: Boolean = false

  override def specialBehavior: Receive = {
    case SyncingDone =>
      logger.info(s"Synced done on Planner.")
      if (settings.canPublishBlocks)
        context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat milliseconds, self, Tick)
      context.become(syncedNode)
    case keyBlock: KeyBlock =>
      lastBlock = keyBlock
      if (keyBlock.scheduler.nonEmpty) {
        epoch = Epoch(lastBlock, keyBlock.scheduler.toSet, settings.epochMultiplier)
      } else epoch.dropNextPublisherPublicKey
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
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
    case MyPublicKey(key) =>
      logger.info("Get key")
      allPublicKeys = allPublicKeys + key
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      myPublicKey = key
    case Tick if epoch.isDone =>
      logger.info(s"epoch.isDone. Height of last block is: ${lastBlock.height}")
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      hasWritten = false
      epoch = Epoch(lastBlock, allPublicKeys, settings.epochMultiplier)
      logger.info(s"New epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      scheduleForWriting = epoch.schedule
      checkMyTurn(isFirstBlock = true, scheduleForWriting)
    case Tick if nextPeriod.timeToPublish =>
      checkMyTurn(isFirstBlock = false, List())
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      checkScheduleUpdateTime()
      logger.info(s"nextPeriod.timeToPublish. Height of last block is: ${lastBlock.height}")
    case Tick if nextPeriod.noBlocksInTime =>
      logger.info(s"nextPeriod.noBlocksInTime. Height of last block is: ${lastBlock.height}")
      epoch = epoch.dropNextPublisherPublicKey
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
      if (!hasWritten) checkMyTurn(isFirstBlock = true, scheduleForWriting)
      else checkMyTurn(isFirstBlock = false, List())
      nextPeriod = Period(nextPeriod, settings)
      context.parent ! nextPeriod
      checkScheduleUpdateTime()
    case Tick =>
      logger.info("123")
      logger.info(s"Current epoch is: $epoch")
      logger.info(s"Current public keys: ${allPublicKeys.map(EncodingUtils.encode2Base16).mkString(",")}")
  }

  def checkMyTurn(isFirstBlock: Boolean, schedule: List[ByteString]): Unit = {
    if (epoch.publicKeyOfNextPublisher == myPublicKey) publisher ! RequestForNewBlock(isFirstBlock, schedule)
    context.parent ! ExpectedBlockPublicKeyAndHeight(epoch.publicKeyOfNextPublisher)
    epoch = epoch.dropNextPublisherPublicKey
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

  case class Epoch(schedule: List[ByteString]) {

    def isApplicableBlock(block: KeyBlock): Boolean = block.publicKey == schedule.head

    def publicKeyOfNextPublisher: ByteString = schedule.head

    def dropNextPublisherPublicKey: Epoch = if (schedule.nonEmpty) this.copy(schedule.tail) else this

    def isDone: Boolean = this.schedule.isEmpty

    def prepareNextEpoch: Boolean = schedule.size <= 2

    override def toString: String = this.schedule.map(EncodingUtils.encode2Base16).mkString(",")
  }

  object Epoch extends StrictLogging {

    def apply(lastKeyBlock: KeyBlock, publicKeys: Set[ByteString], multiplier: Int = 1): Epoch =
      Epoch((1 to multiplier).foldLeft(publicKeys.toList) { case (a, _) => a ::: a })
  }

  case object Tick

}