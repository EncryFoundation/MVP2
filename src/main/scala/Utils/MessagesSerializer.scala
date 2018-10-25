package Utils

import Messages.{KnownPeers, NetworkMessage, Ping, Pong}
import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.util.ByteString

object MessagesSerializer {

  def toBytes(message: NetworkMessage)(implicit system: ActorSystem): ByteString = {
    val serialization = SerializationExtension(system)
    ByteString(message match {
      case ping: Ping.type => Ping.typeId +: serialization.findSerializerFor(Ping).toBinary(ping)
      case pong: Pong.type => Pong.typeId +: serialization.findSerializerFor(Pong).toBinary(pong)
      case knownPeers: KnownPeers =>
        KnownPeers.typeId +: serialization.findSerializerFor(KnownPeers).toBinary(knownPeers)
    })
  }

  def fromBytes(bytes: Array[Byte])(implicit system: ActorSystem): Option[NetworkMessage] = {
    val serialization = SerializationExtension(system)
    bytes.head match {
      case Ping.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.tail)).map{
        case ping: Ping.type => ping
      }
      case Pong.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.tail)).map{
        case pong: Pong.type => pong
      }
      case KnownPeers.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.tail)).map{
        case knownPeers: KnownPeers => knownPeers
      }
    }
  }
}
