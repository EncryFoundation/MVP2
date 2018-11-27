package mvp2.data

import java.net.InetSocketAddress

import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import mvp2.data.NetworkMessages.Peers
import mvp2.utils.Settings
import org.scalatest.{Matchers, PropSpecLike}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

class KnownPeersTest extends PropSpecLike with Matchers {

  val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load)
    .as[Settings]("mvp")

  val peers: KnownPeers = KnownPeers(settings)

  val fakePeer1: (InetSocketAddress, ByteString) =
    new InetSocketAddress("192.168.1.1", 1234) -> ByteString("PubKey1")
  val fakePeer2: (InetSocketAddress, ByteString) =
    new InetSocketAddress("192.168.1.2", 1234) -> ByteString("PubKey2")
  val fakePeer3: (InetSocketAddress, ByteString) =
    new InetSocketAddress("192.168.1.3", 1234) -> ByteString("PubKey3")
  val fakePeer4: (InetSocketAddress, ByteString) =
    new InetSocketAddress("192.168.1.4", 1234) -> ByteString("PubKey4")
  val fakePeer5: (InetSocketAddress, ByteString) =
    new InetSocketAddress("192.168.1.5", 1234) -> ByteString("PubKey5")

  val fakePeers: Map[InetSocketAddress, ByteString] = Map(
    fakePeer1, fakePeer2, fakePeer3, fakePeer4, fakePeer5
  )

  val fakeMyNode: (InetSocketAddress, ByteString) =
    new InetSocketAddress("192.168.1.6", 1234) -> ByteString("MyPubKey")

  val msgWithFakePeers = Peers(fakePeers, fakeMyNode, fakeMyNode._1)

  property("KnownPeers should add peer after knownPeers msg and should ignore myAddr") {

    val newPeers = peers.updatePeers(msgWithFakePeers, fakeMyNode._1)

    newPeers.peersPublicKeyMap.size shouldEqual peers.peersPublicKeyMap.size + fakePeers.size
  }

  property("KnownPeers should get all keys of all online nodes") {

    val newPeers = peers.updatePeers(msgWithFakePeers, fakeMyNode._1).cleanPeersByTime

    newPeers.getPeersKeys.length shouldEqual fakePeers.size
  }

  property("Known peers should ignore peers with lastResponseTime > System.currentTimeMillis() - settings.blockPeriod") {

    val newPeers = peers.updatePeers(msgWithFakePeers, fakeMyNode._1)

    Thread.sleep(settings.blockPeriod + 10)

    val msgWithFakePeersTail = Peers(fakePeers.tail, fakeMyNode, fakeMyNode._1)

    newPeers.updatePeers(msgWithFakePeersTail, fakeMyNode._1)
      .cleanPeersByTime
      .getPeersKeys
      .length shouldEqual fakePeers.size - 1
  }

  property("Known peers should return keys of nodes with equal peers msg") {

    val newPeers = peers.updatePeers(Peers(Map(fakePeer1, fakePeer2, fakePeer3), fakeMyNode, fakeMyNode._1), fakeMyNode._1)

    val msg1WithFakePeers = Peers(Map(fakePeer2, fakePeer3), fakePeer1, fakePeer1._1)
    val msg2WithFakePeers = Peers(Map(fakePeer1, fakePeer3), fakePeer2, fakePeer2._1)
    val msg3WithFakePeers = Peers(Map(fakePeer1, fakePeer2), fakePeer3, fakePeer3._1)
    val msg4WithFakePeers = Peers(Map(fakePeer4), fakePeer4, fakePeer4._1)

    Seq(msg1WithFakePeers, msg2WithFakePeers, msg3WithFakePeers, msg4WithFakePeers)
      .foldLeft(newPeers) {
        case (knownPeers, nextMsg) => knownPeers.checkPeersIdentity(nextMsg, fakeMyNode._1)
      }.cleanPeersByIdenticalKnownPeers
      .cleanPeersByTime
      .getPeersKeys
      .length shouldEqual 3
  }
}
