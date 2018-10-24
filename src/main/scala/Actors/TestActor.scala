package Actors

import Actors.InfluxActor.TestMessage
import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging

class TestActor extends Actor with StrictLogging {

  import Actors.TestActor.Message

  val influxActorRef: ActorRef = context.actorOf(Props(classOf[InfluxActor]), "influx")

  override def receive: Receive = {
    case message: Message => println(message.info)
      logger.info(message.info)
      influxActorRef ! TestMessage
  }

}

object TestActor {

  case class Message(info: String)

}