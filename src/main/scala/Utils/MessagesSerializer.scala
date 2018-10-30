package Utils

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.util.ByteString
import mvp2.Messages._
import mvp2.MVP2.system

object MessagesSerializer {

  val serialization = SerializationExtension(system)

  def toBytes(message: NetworkMessage)(implicit system: ActorSystem): ByteString = {
    ByteString(message match {
      case ping: Ping.type => Ping.typeId +: serialization.findSerializerFor(Ping).toBinary(ping)
      case pong: Pong.type => Pong.typeId +: serialization.findSerializerFor(Pong).toBinary(pong)
      case knownPeers: KnownPeers =>
        KnownPeers.typeId +: serialization.findSerializerFor(KnownPeers).toBinary(knownPeers)
      case blocks: BlocksToNet =>
        BlocksToNet.typeId +: serialization.findSerializerFor(blocks).toBinary(blocks)
    })
  }

  def fromBytes(bytes: Array[Byte])(implicit system: ActorSystem): Option[NetworkMessage] = {
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
      case BlocksToNet.typeId => Option(serialization.findSerializerFor(BlocksToNet).fromBinary(bytes.tail)).map{
        case blocks: BlocksToNet => blocks
      }
    }
  }
}
