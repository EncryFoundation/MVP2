package mvp2.actors

import java.security.{KeyPair, PublicKey}
import akka.actor.{ActorRef, ActorSelection, Cancellable, Props}
import akka.util.ByteString
import mvp2.data.KeyBlock
import mvp2.messages.{Get, NewPublisher}
import mvp2.utils.{ECDSA, Settings}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class Planner(settings: Settings) extends CommonActor {

  import Planner.{Period, Tick}

  val heartBeat: Cancellable =
    context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat milliseconds, self, Tick)
  var nextTurn: Period = Period(KeyBlock(), settings)
  var publishersPubKeys: Set[ByteString] = Set.empty
  val keyKeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  val myKeys: KeyPair = ECDSA.createKeyPair
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      println(s"Planner received new keyBlock with height: ${keyBlock.height}.")
      nextTurn = Period(keyBlock, settings)
      context.parent ! nextTurn
    case newPublisher: NewPublisher =>
      logger.info(s"Planner knows about new poblisher in the network (${newPublisher.publicKey}) " +
        s"and adds him into next schedule.")
      publishersPubKeys += newPublisher.publicKey
    case Tick if nextTurn.timeToPublish =>
      publisher ! Get
      println("Planner send publisher request: time to publish!")
    case Tick if nextTurn.noBlocksInTime =>
      val newPeriod = Period(nextTurn, settings)
      println(s"No blocks in time. Planner added ${newPeriod.exactTime - System.currentTimeMillis} milliseconds.")
      nextTurn = newPeriod
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

  case object Tick

}