package mvp2.actors

import java.security.KeyPair
import akka.util.ByteString
import mvp2.messages.Get
import mvp2.utils.ECDSA

class KeyKeeper extends CommonActor {

  val myKeys: KeyPair = ECDSA.createKeyPair

  var nodesKeys: Set[ByteString] = Set.empty

  override def specialBehavior: Receive = {
    case Get => sender ! myKeys.getPublic
  }
}
