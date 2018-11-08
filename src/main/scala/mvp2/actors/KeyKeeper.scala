package mvp2.actors

import java.security.{KeyPair, PublicKey}
import mvp2.messages.{Get, PeerPublicKey}
import mvp2.utils.ECDSA

class KeyKeeper extends CommonActor {

  val myKeys: KeyPair = ECDSA.createKeyPair

  var nodesKeys: Set[PublicKey] = Set.empty

  override def specialBehavior: Receive = {
    case Get => sender ! myKeys.getPublic
    case PeerPublicKey(key) => nodesKeys += key
  }
}
