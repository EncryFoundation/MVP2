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

  var lastKeyBlock: KeyBlock = KeyBlock()
  var nextTurn: Period = Period()
  var publishersPubKeys: Set[ByteString] = Set.empty
  val heartBeat: Cancellable =
    context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat seconds, self, Tick)
  val keyKeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  val myKeys: KeyPair = ECDSA.createKeyPair
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      logger.info(s"Planner received new keyBlock with height: ${keyBlock.height}.")
      lastKeyBlock = keyBlock
      nextTurn = Period(keyBlock, settings)
      context.parent ! nextTurn
    case newPublisher: NewPublisher =>
      logger.info(s"Planner knows about new poblisher in the network (${newPublisher.publicKey}) " +
        s"and adds him into next schedule.")
      publishersPubKeys += newPublisher.publicKey
    case Tick if timeToPublish(nextTurn) => publisher ! Get
    case Tick => logger.info("Planner is working in vain.")
  }

  def timeToPublish(nextTurn: Period): Boolean = {
    val currentTime: Long = System.currentTimeMillis
    currentTime >= nextTurn.exactTime && currentTime <= nextTurn.end
  }
}

object Planner {

  case class Period(begin: Long = 0, exactTime: Long = 0, end: Long = 0)

  object Period {

    def apply(keyBlock: KeyBlock, settings: Settings): Period = {
      val exactTimestamp: Long = keyBlock.timestamp + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }
  }

  case object Tick

}