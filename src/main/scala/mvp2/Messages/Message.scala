package mvp2.Messages

import java.net.InetSocketAddress
import akka.actor.ActorRef
import akka.util.ByteString

sealed trait Message

case object Get extends Message

final case class InfoMessage(info: String) extends Message

case class CurrentBlockchainInfo(height: Int,
                                 lastGeneralBlock: Option[ByteString],
                                 lastMicroBlock: Option[ByteString]) extends Message

sealed trait NetworkMessage extends Message

case object Ping extends NetworkMessage {

  val typeId: Byte = 1: Byte
}

case object Pong extends NetworkMessage {

  val typeId: Byte = 2: Byte
}

case class KnownPeers(peers: List[InetSocketAddress], remote: InetSocketAddress) extends NetworkMessage

object KnownPeers {

  val typeId: Byte = 3: Byte
}

case class Blocks(blocks: Seq[Blocks]) extends NetworkMessage

object Blocks {

  val typeId: Byte = 4: Byte
}

case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class UdpSocket(conection: ActorRef) extends Message

