package Messages

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

case class Ping(remote: InetSocketAddress) extends NetworkMessage

case class Pong(remote: InetSocketAddress) extends NetworkMessage

case class KnownPeers(peers: List[InetSocketAddress], remote: InetSocketAddress) extends NetworkMessage

case object BroadcastPeers extends NetworkMessage

case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends Message

case class UdpSocket(conection: ActorRef) extends Message

