package mvp2.messages

import java.net.InetSocketAddress
import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.data.Chain

sealed trait Message

case object Get extends Message

final case class InfoMessage(info: String) extends Message

final case class CurrentBlockchainInfo(height: Int,
                                       lastGeneralBlock: Option[ByteString],
                                       lastMicroBlock: Option[ByteString]) extends Message

sealed trait NetworkMessage extends Message

case object Ping extends NetworkMessage {

  val typeId: Byte = 1: Byte
}

case object Pong extends NetworkMessage {

  val typeId: Byte = 2: Byte
}

case class Peers(peers: List[InetSocketAddress], remote: InetSocketAddress) extends NetworkMessage

case object Peers {

  val typeId: Byte = 3: Byte
}

case class Blocks(chain: Chain) extends NetworkMessage

object Blocks {

  val typeId: Byte = 4: Byte
}

case object Bye extends NetworkMessage {

  val typeId: Byte = 5: Byte
}

case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class UdpSocket(conection: ActorRef) extends Message

case object GetPeersForSchedule extends Message

case class PeersForSchedule(peers: List[InetSocketAddress]) extends Message