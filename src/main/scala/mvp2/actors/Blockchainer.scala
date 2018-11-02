package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import java.security.PublicKey
import akka.actor.{ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data._
import mvp2.messages._
import mvp2.utils.{ECDSA, EncodingUtils, Settings}
import scala.concurrent.duration._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging with Blockchain {

  var appendix: Appendix = Appendix(TreeMap())
  val accountant: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/accountant")
  val networker: ActorSelection = context.system.actorSelection("/user/starter/networker")
  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)
  var schedule: List[InetSocketAddress] = List.empty
  val publicKeys: Map[InetSocketAddress, PublicKey] = Map(
    new InetSocketAddress("172.16.10.56", 9003) ->
      ECDSA.uncompressPublicKey(
        EncodingUtils.decodeFromBase64("A+WiAs/0/4xL8K1LfDA5i+is5QUlmHs9JT3nHacu9UQ/")
      ),
    new InetSocketAddress("172.16.10.57", 9003) ->
      ECDSA.uncompressPublicKey(
        EncodingUtils.decodeFromBase64("Ah+fDMBeY8efVxnCHwam3KK01n8FNF1vKw9S7PJ1k1WM")
      ),
    new InetSocketAddress("172.16.10.58", 9003) ->
    ECDSA.uncompressPublicKey(EncodingUtils.decodeFromBase64("AwiInhz8RfxaqgJ7jmRuhVOJGbe9dK7VUr1SvWGwsUaV"))
  )

  var lastBlockTime: Long = 0

  context.actorOf(Props(classOf[Accountant]), "accountant")

  override def preStart(): Unit = {
    networker ! GetPeersForSchedule
    context.system.scheduler.schedule(10.seconds, settings.blockchain.blockInterval.seconds) {
      logger.info(s"Current schedule: ${schedule.mkString(",")}")
      if (schedule.size == 1) networker ! GetPeersForSchedule
    }
    context.system.scheduler.schedule(10.seconds, settings.blockchain.blockInterval.seconds) {
      if (System.currentTimeMillis - settings.blockchain.blockDelta > lastBlockTime && schedule.headOption.contains(myAddr)) {
        logger.info("Looks like it is my tern to publish block")
        context.actorSelection("/user/starter/blockchainer/publisher") ! PublishBlock
      }
      else if (System.currentTimeMillis - lastBlockTime > settings.blockchain.blockDelta + settings.blockchain.blockDelta) {
        schedule = schedule.tail
        logger.info("Looks like no one produce block set next guy")
        lastBlockTime = System.currentTimeMillis
      }
    }
  }

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => update(keyBlock)
    case microBlock: MicroBlock => update(microBlock)
    case RecoveryCompleted =>
      context.actorOf(Props(classOf[Publisher], settings), "publisher")
      context.actorSelection("/user/starter/blockchainer/publisher") ! lastKeyBlock.getOrElse(
        KeyBlock(0, System.currentTimeMillis(), ByteString.empty, List())
      )
  }

  override def receiveCommand: Receive = {
    case block: Block => saveModifier(block)
    case blocks: List[Block] => blocks.foreach(saveModifier)
    case PeersForSchedule(peers) =>
      prepareSchedule(peers)
    case Get => chain
    case _ => logger.info("Got something strange at Blockchainer!")
  }

  def saveModifier(block: Block): Unit = {
    if (block.isValid) {
      block match {
        case keyBlock: KeyBlock =>
          logger.info(s"KeyBlock is valid with height ${keyBlock.height}.")
          appendix.chain.foreach(block =>
            persist(block._2) { x =>
              logger.info(s"Successfully saved block with id: ${x.hash} and height ${x.height}!")
            })
          update(appendix.chain)
          appendix = appendix.copy(TreeMap(keyBlock.height -> keyBlock))
          accountant ! keyBlock
          lastBlockTime = System.currentTimeMillis()
          schedule = schedule.tail
        case microBlock: MicroBlock =>
          logger.info(s"KeyBlock is valid with height ${microBlock.height}.")
          appendix = appendix.copy(appendix.chain + (microBlock.height -> microBlock))
          accountant ! microBlock
      }
    }
  }

  override def persistenceId: String = "blockchainer"

  override def journalPluginId: String = "akka.persistence.journal.leveldb"

  override def snapshotPluginId: String = "akka.persistence.snapshot-store.local"

  def prepareSchedule(peers: List[InetSocketAddress]): Unit = {
    logger.info("Update scheduler")
    val sortedList = peers.sortBy(_.getHostName)
    schedule = schedule ++ (0 until settings.blockchain.epochMultiplier).foldLeft(List[InetSocketAddress]()){
      case (nextSchedule, _) => nextSchedule ++ sortedList
    }
  }
}