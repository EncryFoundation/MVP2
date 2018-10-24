package Actors

import akka.actor.Actor

trait CommonActor extends Actor {

  def specialBehavior: Receive

  def smth: Receive = {
    case smth: Any => println(s"Got smth strange: $smth.")
  }

  override def receive: Receive = specialBehavior orElse smth
}
