package mvp2.actors

import java.security.KeyPair
import mvp2.messages.Get
import mvp2.utils.ECDSA

class KeyKeeper extends CommonActor {

  val myKeys: KeyPair = ECDSA.createKeyPair

  override def specialBehavior: Receive = {
    case Get => sender ! myKeys.getPublic
  }
}
