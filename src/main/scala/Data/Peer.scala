package Data

import java.net.InetSocketAddress

case class Peer(remoteAddress: InetSocketAddress,
                lastMessageTime: Long)