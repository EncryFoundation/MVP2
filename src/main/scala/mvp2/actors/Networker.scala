package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{ActorSelection, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.util.ByteString
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages._
import mvp2.data.{KeyBlock, KnownPeers, Transaction}
import mvp2.utils.Settings
import scala.language.postfixOps

class Networker(settings: Settings) extends CommonActor {

  var myPublicKey: Option[ByteString] = None

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  val keyKeeper: ActorSelection = context.actorSelection("/user/starter/blockchainer/planner/keyKeeper")

  val udpSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/udpSender")

  val publisher: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/publisher")

  val influxActor: ActorSelection = context.actorSelection("/user/starter/influxActor")

  val planner: ActorSelection = context.system.actorSelection("/user/starter/blockchainer/planner")

  var peers: KnownPeers = KnownPeers(settings)

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    context.system.scheduler.schedule(1.seconds, settings.heartbeat.millisecond)(sendPeers())
    bornKids()
  }

  override def specialBehavior: Receive = {
    case msgFromNetwork: MsgFromNetwork =>
      msgFromNetwork.message match {
        case msg@Peers(peersFromRemote, _) =>
          peersFromRemote.foreach(peer => updatePeerKey(peer.publicKey))
          peers = peers.updatePeers(msg, myAddr)
        case Blocks(blocks) =>
          if (blocks.nonEmpty) context.parent ! msgFromNetwork.message
        case SyncMessageIterators(iterators) =>
          influxActor ! SyncMessageIteratorsFromRemote(
            iterators.map(iterInfo => iterInfo.msgName -> iterInfo.msgIter).toMap,
            msgFromNetwork.remote)
        case LastBlockHeight(height) => context.parent ! CheckRemoteBlockchain(height, msgFromNetwork.remote)
        case Transactions(transactions) =>
          logger.info(s"Got ${transactions.size} new transactions.")
          transactions.foreach(tx => publisher ! tx)
      }
    case OwnBlockchainHeight(height) => peers.getHeightMessage(height).foreach(udpSender ! _)
    case MyPublicKey(key) => myPublicKey = Some(key)
    case PrepareScheduler => self ! PrepareSchedulerStep(settings.network.qtyOfPrepareSchedulerSteps)
    case PrepareSchedulerStep(currentStep) =>
      sendPeers()
      if (currentStep != 0)
        context.system.scheduler
          .scheduleOnce((settings.blockPeriod / 10) milliseconds)(self ! PrepareSchedulerStep(currentStep - 1))
      else {
        peers = peers.cleanPeersByTime
        planner ! KeysForSchedule(peers.getPeersKeys)
      }
    case transaction: Transaction => peers.getTransactionMsg(transaction).foreach(msg => udpSender ! msg)
    case keyBlock: KeyBlock => peers.getBlockMessage(keyBlock).foreach(udpSender ! _)
    case RemoteBlockchainMissingPart(blocks, remote) =>
      udpSender ! SendToNetwork(Blocks(blocks), remote)
  }

  def updatePeerKey(serializedKey: ByteString): Unit =
    if (!myPublicKey.contains(serializedKey)) keyKeeper ! PeerPublicKey(serializedKey)

  def sendPeers(): Unit =
    myPublicKey.foreach(key => peers.getPeersMessages(myAddr, key).foreach(msg => udpSender ! msg))

  def bornKids(): Unit = {
    context.actorOf(Props(classOf[UdpReceiver], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "udpReceiver")
    context.actorOf(Props(classOf[UdpSender], settings).withDispatcher("net-dispatcher")
      .withMailbox("net-mailbox"), "udpSender")
  }
}