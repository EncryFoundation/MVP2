package Actors

import akka.actor.Actor

class TestActor extends Actor{

  import Actors.TestActor.Message

  override def receive: Receive = {
    case message: Message => println(message.info)
  }

}

object TestActor {
  case class Message (info: String)
}
