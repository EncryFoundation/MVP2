package Actors

import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging

class TestActor extends Actor with StrictLogging {

  import Actors.TestActor.Message

  override def receive: Receive = {
    case message: Message => println(message.info)
      logger.info(message.info)
  }

}

object TestActor {

  case class Message(info: String)

}