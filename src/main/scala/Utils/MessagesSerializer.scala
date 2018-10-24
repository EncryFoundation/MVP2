package Utils

import java.net.InetSocketAddress
import MVP.MVP2.system
import Messages.KnownPeers
import akka.serialization.{SerializationExtension, Serializer}

object MessagesSerializer {

  object KnownPeersSerializer {

    val serialization = SerializationExtension(system)
    val serializer: Serializer = serialization.findSerializerFor(List[InetSocketAddress]())

    def toBytes(message: KnownPeers): Array[Byte] = serializer.toBinary(message)

    def fromBytes(bytes: Array[Byte]): KnownPeers = serializer.fromBinary(bytes).asInstanceOf[KnownPeers]
  }

}
