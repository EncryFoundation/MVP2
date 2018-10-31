package Utils

import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import akka.util.ByteString
import mvp2.Messages._
import mvp2.MVP2.system

object MessagesSerializer {

  val serialization: Serialization = SerializationExtension(system)

  def serialize(message: NetworkMessage): ByteString = {
    ByteString(message match {
      case ping: Ping.type => Ping.typeId +: serialization.findSerializerFor(Ping).toBinary(ping)
      case pong: Pong.type => Pong.typeId +: serialization.findSerializerFor(Pong).toBinary(pong)
      case knownPeers: Peers =>
        Peers.typeId +: serialization.findSerializerFor(Peers).toBinary(knownPeers)
      case blocks: Blocks =>
        Blocks.typeId +: serialization.findSerializerFor(blocks).toBinary(blocks)
    })
  }

  def deserialize(bytes: Array[Byte]): Option[NetworkMessage] = {
    bytes.head match {
      case Ping.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.tail)).map{
        case ping: Ping.type => ping
      }
      case Pong.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.tail)).map{
        case pong: Pong.type => pong
      }
      case Peers.typeId => Option(serialization.findSerializerFor(Ping).fromBinary(bytes.tail)).map{
        case knownPeers: Peers => knownPeers
      }
      case Blocks.typeId => Option(serialization.findSerializerFor(Blocks).fromBinary(bytes.tail)).map{
        case blocks: Blocks => blocks
      }
    }
  }
}
