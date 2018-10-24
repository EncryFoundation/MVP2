package encry.Actors

class Networker extends CommonActor {

  import encry.Messages.InfoMessage

  override def preStart(): Unit = {
    println("Starting the Networker!")
    bornKids()
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => println(message.info)
  }

  def bornKids(): Unit = {
    //context.actorOf(Props[UDPActor].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "udp")
    //context.actorOf(Props[UdpReceiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
    //context.actorOf(Props[UdpSender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
  }
}

