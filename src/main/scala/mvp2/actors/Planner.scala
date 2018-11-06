package mvp2.actors

import akka.actor.Cancellable
import akka.util.ByteString
import mvp2.data.KeyBlock
import mvp2.messages.NewPublisher
import mvp2.utils.Settings
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(myPublicKey: ByteString, settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  var publishersPubKeys: Set[ByteString] = Set(myPublicKey)
  var lastKeyBlock: KeyBlock = KeyBlock()
  var nextTurn: Period = Period()

  val cancellable: Cancellable =
    context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat seconds, self, Tick)

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      logger.info(s"Planner received new keyBlock with height: ${keyBlock.height}.")
      lastKeyBlock = keyBlock
      nextTurn = Period(keyBlock, settings)
    case newPublisher: NewPublisher =>
      logger.info(s"Planner knows about new poblisher in the network (${newPublisher.publicKey}) " +
        s"and adds him into next schedule.")
      publishersPubKeys += newPublisher.publicKey
    case Tick =>
  }

  def isTime(nextTurn: Period): Boolean = {
    val currentTime: Long = System.currentTimeMillis
    currentTime >= nextTurn.exactTime && currentTime <= nextTurn.end
  }
}

object Planner {

  case class Period(begin: Long = 0, exactTime: Long = 0, end: Long = 0) {

  }

  object Period {

    def apply(keyBlock: KeyBlock, settings: Settings): Period = {
      val exactTimestamp: Long = keyBlock.timestamp + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }
  }

  case object Tick

}