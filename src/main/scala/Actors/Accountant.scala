package Actors

import akka.util.ByteString

class Accountant extends CommonActor {

  var state: State = ???

  override def specialBehavior: Receive = ???

}

case class State(accounts: List[Account])

case class Account(publicKey: ByteString, data: List[ByteString], amount: Int)
