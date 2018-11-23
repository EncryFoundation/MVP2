package mvp2.data

import java.net.InetSocketAddress
import java.security.PublicKey
import akka.actor.ActorRef
import akka.util.ByteString
import mvp2.data.NetworkMessages.NetworkMessage
import io.circe.syntax._
import io.circe.generic.auto._
import mvp2.utils.EncodingUtils._

object InnerMessages {

  sealed trait InnerMessage

  case object Get extends InnerMessage

  final case class CurrentBlockchainInfo(height: Long = 0,
                                         lastKeyBlock: Option[KeyBlock] = None,
                                         lastMicroBlock: Option[ByteString] = None) extends InnerMessage {

    override def toString: String = s"Height: $height, last keyBlock: ${lastKeyBlock.getOrElse("None")}, " +
      s"last microBlock: $lastMicroBlock."
  }

  final case class SendToNetwork(message: NetworkMessage, remote: InetSocketAddress) extends InnerMessage

  final case class MsgToNetwork(message: NetworkMessage, id: ByteString, remote: InetSocketAddress) extends InnerMessage

  final case class MsgFromNetwork(message: NetworkMessage, remote: InetSocketAddress,
                                  id: ByteString = ByteString.empty) extends InnerMessage

  final case class SyncMessageIteratorsFromRemote(iterators: Map[String, Int], remote: InetSocketAddress) extends InnerMessage

  final case class UdpSocket(conection: ActorRef) extends InnerMessage

  final case class PeerPublicKey(peerPublicKey: ByteString) extends InnerMessage

  final case class MyPublicKey(publicKey: ByteString) extends InnerMessage

  final case class ExpectedBlockSignatureAndHeight(height: Long, signature: ByteString) extends InnerMessage

  final case class TimeDelta(delta: Long) extends InnerMessage

  final case object GetLightChain extends InnerMessage

  final case class OwnBlockchainHeight(height: Long) extends InnerMessage

  final case class CheckRemoteBlockchain(remoteHeight: Long, remote: InetSocketAddress) extends InnerMessage

  final case class RemoteBlockchainMissingPart(blocks: List[KeyBlock], remote: InetSocketAddress) extends InnerMessage

  final case object SyncingDone extends InnerMessage
}