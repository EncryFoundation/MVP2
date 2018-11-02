package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{ActorSelection, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.data._
import mvp2.messages._
import mvp2.utils.Settings
import scala.concurrent.duration._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global

class Blockchainer(settings: Settings) extends PersistentActor with StrictLogging with Blockchain {

  var appendix: Appendix = Appendix(TreeMap())
  val accountant: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/accountant")
  val networker: ActorSelection = context.system.actorSelection("/user/starter/networker")
  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)
  var schedule: List[InetSocketAddress] = List.empty

  var lastBlockTime: Long = 0

  context.actorOf(Props(classOf[Accountant]), "accountant")

  override def preStart(): Unit = {
    networker ! GetPeersForSchedule
    context.system.scheduler.schedule(10.seconds, 5.seconds) {
      logger.info(s"Current schedule: ${schedule.mkString(",")}")
    }
    context.system.scheduler.schedule(10.seconds, settings.blockchain.blockInterval.seconds) {
      if (lastBlockTime > System.currentTimeMillis + settings.blockchain.blockDelta + settings.blockchain.blockDelta) {
        schedule = schedule.tail
        lastBlockTime = System.currentTimeMillis
      }
      else if (schedule.isEmpty) {
        logger.info("Try to update modifier")
        networker ! GetPeersForSchedule
      }
      else if (System.currentTimeMillis - settings.blockchain.blockDelta > lastBlockTime && schedule.head == myAddr) {
        logger.info("Looks like it is my tern to publish block")
        context.actorSelection("/user/starter/blockchainer/publisher") ! PublishBlock
      }
    }
  }

  override def receiveRecover: Receive = {
    case keyBlock: KeyBlock => update(keyBlock)
    case microBlock: MicroBlock => update(microBlock)
    case RecoveryCompleted =>
      context.actorOf(Props[Publisher], "publisher")
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
              logger.info(s"Successfully saved block with id: ${x.currentBlockHash} and height ${x.height}!")
            })
          update(appendix.chain)
          appendix = appendix.copy(TreeMap(keyBlock.height -> keyBlock))
          accountant ! keyBlock
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