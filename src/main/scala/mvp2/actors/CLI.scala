package mvp2.actors

import akka.actor.Actor
import scala.concurrent.duration._
import scala.concurrent.Await

class CLI extends Actor {

  override def preStart(): Unit = {
    scala.io.Source.stdin.getLines().map(cliCommands).foreach(println)
    println("Started CLI")
  }

  val cliCommands: PartialFunction[String, Unit] = {
    case "node shutdown" =>
      context.system.ch
    case _ => println(s"Wrong command!")
  }

  override def receive: Receive = {
    case _ => println(s"Got something wrong!")
  }
}