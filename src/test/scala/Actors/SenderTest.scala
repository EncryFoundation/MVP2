package Actors

import java.net.InetSocketAddress
import Messages.{Ping, Pong, UdpSocket}
import Utils.MessagesSerializer
import akka.actor.{ActorSystem, Props}
import akka.io.Udp
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class SenderTest extends TestKit(ActorSystem("SenderTestSystem")) with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val probe = TestProbe()
  val sender = system.actorOf(Props[Sender], "Sender")
  val addr: InetSocketAddress = new InetSocketAddress("localhost", 5678)

  it should "send ping message to udp.socket when receive case object Ping" in {
    sender ! UdpSocket(probe.ref)
    sender ! Ping
    probe.expectMsg(Udp.Send(MessagesSerializer.toBytes(Ping), addr))
  }

  it should "send pong message to udp.socket when receive case object Pong" in {
    sender ! UdpSocket(probe.ref)
    sender ! Pong
    probe.expectMsg(Udp.Send(MessagesSerializer.toBytes(Pong), addr))
  }
}
