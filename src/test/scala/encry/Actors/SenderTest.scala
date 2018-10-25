package encry.Actors

import java.net.InetSocketAddress
import encry.Messages.{Ping, Pong, UdpSocket}
import akka.actor.{ActorSystem, Props}
import akka.io.Udp
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import encry.Actors.Sender
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class SenderTest extends TestKit(ActorSystem("SenderTestSystem")) with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val probe = TestProbe()
  val sender = system.actorOf(Props[Sender], "Sender")

  it should "send ping message to udp.socket when receive case object Ping" in {
    sender ! UdpSocket(probe.ref)
    sender ! Ping
    probe.expectMsg(Udp.Send(ByteString("Ping"), new InetSocketAddress("localhost", 1234)))
  }

  it should "send pong message to udp.socket when receive case object Pong" in {
    sender ! UdpSocket(probe.ref)
    sender ! Pong
    probe.expectMsg(Udp.Send(ByteString("Pong"), new InetSocketAddress("localhost", 1234)))
  }
}
