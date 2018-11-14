package mvp2.data

import java.net.InetSocketAddress

import akka.util.ByteString

case class Peer(remoteAddress: InetSocketAddress,
                lastMessageTime: Long,
                key: Option[ByteString])
