package mvp2.actors

import akka.actor.{ActorSelection, Cancellable}
import mvp2.data.InnerMessages.Get
import mvp2.data.KeyBlock
import mvp2.utils.Settings
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  val heartBeat: Cancellable =
    context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  var nextPeriod: Period = Period(KeyBlock(), settings)
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      logger.info(s"Planner received new keyBlock with height: ${keyBlock.height}.")
      nextPeriod = Period(keyBlock, settings)
      context.parent ! nextPeriod
    case Tick if nextPeriod.timeToPublish =>
      publisher ! Get
      logger.info("Planner sent publisher request: time to publish!")
    case Tick if nextPeriod.noBlocksInTime =>
      val newPeriod = Period(nextPeriod, settings)
      logger.info(s"No blocks in time. Planner added ${newPeriod.exactTime - System.currentTimeMillis} milliseconds.")
      nextPeriod = newPeriod
    case Tick =>
  }
}

object Planner {

  case class Period(begin: Long, exactTime: Long, end: Long) {
    def timeToPublish: Boolean = {
      val now: Long = System.currentTimeMillis
      now >= this.begin && now <= this.end
    }

    def noBlocksInTime: Boolean = {
      val now: Long = System.currentTimeMillis
      now > this.end
    }
  }

  object Period {

    def apply(lastKeyBlock: KeyBlock, settings: Settings): Period = {
      val exactTimestamp: Long = lastKeyBlock.timestamp + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }

    def apply(previousPeriod: Period, settings: Settings): Period = {
      val exactTimestamp: Long = previousPeriod.exactTime + settings.blockPeriod / 2
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }
  }

  case class Epoch(lastKeyBlock: KeyBlock) {
    val allPeriodInThisEpoch: Map[Int, Period] = _
  }

  case object Tick

}