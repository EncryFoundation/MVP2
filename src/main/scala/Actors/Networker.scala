package Actors

class Networker extends CommonActor {

  import Messages.InfoMessage

  override def preStart(): Unit = {
    println("Starting the Networker!")
    bornKids()
  }

  override def specialBehavior: Receive = {
    case message: InfoMessage => println(message.info)
  }

  def bornKids(): Unit = {
  }
}

