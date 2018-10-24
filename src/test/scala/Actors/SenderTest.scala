package Actors

import java.net.InetSocketAddress
import Messages.{Ping, Pong, UdpSocket}
import akka.actor.{ActorSystem, Props}
import akka.io.Udp
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class SenderTest extends TestKit(ActorSystem("SenderTestSystem")) with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val probe = TestProbe()
  val sender = system.actorOf(Props[Sender], "Sender")
  val addr: InetSocketAddress = new InetSocketAddress("localhost", 5678)

  it should "send ping message to udp.socket when receive case object Ping" in {
    sender ! UdpSocket(probe.ref)
    sender ! Ping(addr)
    probe.expectMsg(Udp.Send(ByteString("Ping"), addr))
  }

  it should "send pong message to udp.socket when receive case object Pong" in {
    sender ! UdpSocket(probe.ref)
    sender ! Pong(addr)
    probe.expectMsg(Udp.Send(ByteString("Pong"), addr))
  }
}
