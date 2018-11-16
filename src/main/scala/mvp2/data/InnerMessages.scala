package mvp2.data

import java.net.InetSocketAddress
import java.security.PublicKey
import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.data.NetworkMessages.NetworkMessage

object InnerMessages {

  sealed trait InnerMessage

  case object Get extends InnerMessage

  final case class CurrentBlockchainInfo(height: Long = 0,
                                         lastKeyBlock: Option[KeyBlock] = None,
                                         lastMicroBlock: Option[ByteString] = None) extends InnerMessage {

    override def toString: String = s"Height: $height, last keyBlock: ${lastKeyBlock.getOrElse("None")}, " +
      s"last microBlock: $lastMicroBlock."
  }

  final case class NewPublisher(publicKey: ByteString) extends InnerMessage

  final case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends InnerMessage

  final case class MsgToNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends InnerMessage

  final case class MsgFromNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends InnerMessage

  final case class MessageFromRemote(message: NetworkMessage, remote: InetSocketAddress) extends InnerMessage

  final case class SyncMessageIteratorsFromRemote(iterators: Map[String, Int], remote: InetSocketAddress) extends InnerMessage

  final case class UdpSocket(conection: ActorRef) extends InnerMessage

  final case class PeerPublicKey(peerPublicKey: PublicKey) extends InnerMessage

  final case class MyPublicKey(publicKey: PublicKey) extends InnerMessage

  final case class TimeDelta(delta: Long) extends InnerMessage

  final case object GetLightChain extends InnerMessage

}