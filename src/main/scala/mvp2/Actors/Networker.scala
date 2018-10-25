package mvp2.Actors

import akka.actor.Props

class Networker extends CommonActor {

  import mvp2.Messages.InfoMessage

  override def preStart(): Unit = {
    logger.info("Starting the Networker!")
    bornKids()
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => logger.info(message.info)
  }

  def bornKids(): Unit = {
    context.actorOf(Props[Sender].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "sender")
    context.actorOf(Props[Receiver].withDispatcher("net-dispatcher").withMailbox("net-mailbox"), "receiver")
  }
}