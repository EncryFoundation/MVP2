package Messages

sealed trait Message

case object Start extends Message

final case class InfoMessage(info: String) extends Message