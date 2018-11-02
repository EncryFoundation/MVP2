package mvp2.actors

import akka.actor.Actor

class CLI extends Actor {

  val promt: String = "$> "

  override def preStart(): Unit = {
    scala.io.Source.stdin.getLines().foreach {
      print(promt)
      cliCommands
    }
  }

  val cliCommands: PartialFunction[String, Unit] = {
    case "node shutdown" => System.exit(0)
    case "help" => println(
       """node shutdown   -   Turn off the application.
         |help            -   Show info about commands.
       """.stripMargin
    )
      print(promt)
    case _ => print(s"Wrong command!\n$promt")
  }

  override def receive: Receive = {
    case _ =>
  }
}