package Utils

import MVP.MVP2.system
import Messages.{KnownPeers, NetworkMessage, Ping, Pong}
import akka.serialization.SerializationExtension
import akka.util.ByteString

object MessagesSerializer {

  val serialization = SerializationExtension(system)

  def toBytes(message: NetworkMessage): ByteString = ByteString(message match {
    case ping: Ping.type => Ping.typeId +: serialization.findSerializerFor(Ping).toBinary(ping)
    case pong: Pong.type => Pong.typeId +: serialization.findSerializerFor(Pong).toBinary(pong)
    case knownPeers: KnownPeers =>
      KnownPeers.typeId +: serialization.findSerializerFor(KnownPeers).toBinary(knownPeers)
  })

  def fromBytes(bytes: Array[Byte]): Option[NetworkMessage] = bytes.head match {
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
