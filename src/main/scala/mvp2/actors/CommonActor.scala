package mvp2.actors

import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging

trait CommonActor extends Actor with StrictLogging {

  def specialBehavior: Receive

  def smth: Receive = {
    case smth: Any => logger.info(s"Got smth strange: $smth.")
  }

  override def receive: Receive = specialBehavior orElse smth

  override def postStop(): Unit = {
    logger.info(s"Actor $self is stopped.")
  }
}
