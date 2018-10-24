package Actors

import Actors.InfluxActor.FirstMessage
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.StrictLogging

class TestActor extends Actor with StrictLogging {

  import Actors.TestActor.Message

  context.actorOf(Props[InfluxActor], "influx")

  override def receive: Receive = {
    case message: Message => println(message.info)
      logger.info(message.info)
      context.actorSelection("/user/influx") ! FirstMessage("init")
  }

}

object TestActor {

  case class Message(info: String)

}
