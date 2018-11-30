package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{Actor, ActorRef, ActorSelection, Terminated}
import akka.io.{IO, Udp}
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import mvp2.MVP2.system
import mvp2.data.InnerMessages.{FromNet, UdpSocket}
import mvp2.data.NetworkMessages._
import mvp2.utils.{EncodingUtils, Settings, Sha256}
import scala.util.Try

class UdpReceiver(settings: Settings) extends Actor with StrictLogging {

  val serialization: Serialization = SerializationExtension(context.system)

  val udpSender: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker/udpSender")

  val influxActor: ActorSelection = context.actorSelection("/user/starter/influxActor")

  val myAddr: InetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost.getHostAddress, settings.port)

  override def preStart(): Unit = {
    logger.info("Starting the Receiver!")
    IO(Udp) ! Udp.Bind(self, myAddr)
  }

  override def receive: Receive = unboundedCycle

  def readCycleWithUnbounded(socket: ActorRef): Receive =
    readCycle(socket) orElse unboundedCycle

  def unboundedCycle: Receive = {
    case Udp.Bound(local) =>
      logger.info(s"Binded to $local")
      context.watch(sender)
      context.become(readCycleWithUnbounded(sender))
      udpSender ! UdpSocket(sender)
    case msg => logger.info(s"Received message $msg from $sender before binding")
  }

  def readCycle(socket: ActorRef): Receive = {
    case Udp.Received(data: ByteString, remote) =>
      deserialize(data).foreach { message =>
        logger.info(s"Received $message from $remote")
        context.parent ! FromNet(message, remote)
        context.actorSelection("/user/starter/influxActor") !
          FromNet(
            message,
            remote,
            Sha256.toSha256(EncodingUtils.encode2Base16(data) ++ myAddr.getAddress.toString)
          )
      }
    case Udp.Unbind =>
      socket ! Udp.Unbind
      logger.info(s"Unbind $socket")
    case Udp.Unbound =>
      logger.info(s"Unbound $socket")
      context.stop(self)
    case Terminated(_) =>
      IO(Udp) ! Udp.Bind(self, myAddr)
  }

  def deserialize(bytes: ByteString): Try[NetworkMessage] = bytes.head match {
    case NetworkMessagesId.PeersId => Peers.parseBytes(bytes.tail)
    case NetworkMessagesId.BlocksId => Blocks.parseBytes(bytes.tail)
    case NetworkMessagesId.SyncMessageIteratorsId => SyncMessageIterators.parseBytes(bytes.tail)
    case NetworkMessagesId.TransactionsId => Transactions.parseBytes(bytes.tail)
    case NetworkMessagesId.LastBlockHeightId => LastBlockHeight.parseBytes(bytes.tail)
    case wrongType => throw new Exception(s"No serializer found for type: $wrongType")
  }
}