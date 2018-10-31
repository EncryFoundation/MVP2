package mvp2.actors

import akka.actor.{Actor, DeadLetter, UnhandledMessage}
import com.typesafe.scalalogging.StrictLogging

class Zombie extends Actor with StrictLogging {

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[DeadLetter])
    context.system.eventStream.subscribe(self, classOf[UnhandledMessage])
  }

  override def receive: Receive = {
    case deadMessage: DeadLetter => logger.info(s"Dead letter: ${deadMessage.toString}.")
    case unhandled: UnhandledMessage => logger.info(s"Unhandled message ${unhandled.toString}")
  }

}