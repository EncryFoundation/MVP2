package mvp2.actors

import java.security.KeyPair
import akka.actor.{ActorRef, Props}
import akka.actor.{ActorSelection, Cancellable}
import akka.util.ByteString
import mvp2.actors.Planner.Epoch
import mvp2.data.InnerMessages.{ExpectedBlockSignatureAndHeight, Get, MyPublicKey, PeerPublicKey}
import mvp2.data.KeyBlock
import mvp2.utils.{ECDSA, Settings}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  var nextTurn: Period = Period(KeyBlock(), settings)
  val keyKeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  val myKeys: KeyPair = ECDSA.createKeyPair
  var myPublicKey: ByteString = ByteString.empty
  var allPublicKeys: Set[ByteString] = Set()
  var nextPeriod: Period = Period(KeyBlock(), settings)
  var lastBlock: KeyBlock = KeyBlock()
  var epoch: Epoch = Epoch(Map())
  val heartBeat: Cancellable =
    context.system.scheduler.schedule(10.seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  if (settings.canPublishBlocks)
    context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat milliseconds, self, Tick)

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      nextPeriod = Period(keyBlock, settings)
      lastBlock = keyBlock
      context.parent ! nextPeriod
    case PeerPublicKey(key) => allPublicKeys = allPublicKeys + key
    case MyPublicKey(key) =>
      allPublicKeys = allPublicKeys + key
      myPublicKey = key
    case Tick if epoch.isDone =>
      epoch = Epoch(lastBlock, allPublicKeys)
      checkMyTurn()
    case Tick if nextPeriod.timeToPublish => checkMyTurn()
    case Tick if nextPeriod.noBlocksInTime =>
      epoch = epoch.noBlockInTime
      nextPeriod = Period(nextPeriod, settings)
      context.parent ! nextPeriod
    case Tick =>
  }

  def checkMyTurn(): Unit =
    if (epoch.nextBlock._2 == myPublicKey) publisher ! Get
    context.parent ! ExpectedBlockSignatureAndHeight(epoch.nextBlock._1, epoch.nextBlock._2)
    epoch = epoch.delete
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