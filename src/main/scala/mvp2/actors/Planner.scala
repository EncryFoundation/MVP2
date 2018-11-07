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
    context.system.scheduler.schedule(0 seconds, settings.plannerHeartbeat seconds, self, Tick)
  var lastKeyBlock: Option[KeyBlock] = None
  var nextTurn: Period = Period(KeyBlock(), settings)
  var publishersPubKeys: Set[ByteString] = Set.empty
  val keyKeeper: ActorRef = context.actorOf(Props(classOf[KeyKeeper]), "keyKeeper")
  val myKeys: KeyPair = ECDSA.createKeyPair
  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  override def specialBehavior: Receive = {
    case keyBlock: KeyBlock =>
      println(s"Planner received new keyBlock with height: ${keyBlock.height}.")
      lastKeyBlock = Option(keyBlock)
      nextTurn = Period(keyBlock, settings)
      context.parent ! nextTurn
    case newPublisher: NewPublisher =>
      logger.info(s"Planner knows about new poblisher in the network (${newPublisher.publicKey}) " +
        s"and adds him into next schedule.")
      publishersPubKeys += newPublisher.publicKey
    case Tick if timeToPublish(nextTurn) =>
      println("Planner is working: time to publish!")
      publisher ! Get
    case Tick => println(s"Planner is working in vain. nexTurn: ${(System.currentTimeMillis - nextTurn.exactTime) / 1000}.")
  }

  def timeToPublish(nextTurn: Period): Boolean = {
    println(nextTurn)
    true
  }
}

object Planner {

  case class Period(begin: Long, exactTime: Long, end: Long)

  object Period {

    def apply(keyBlock: KeyBlock, settings: Settings): Period = {
      val exactTimestamp: Long = keyBlock.timestamp + settings.blockPeriod
      Period(exactTimestamp - settings.biasForBlockPeriod, exactTimestamp, exactTimestamp + settings.biasForBlockPeriod)
    }
  }

  case object Tick

}