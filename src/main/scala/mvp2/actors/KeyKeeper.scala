package mvp2.actors

import java.security.{KeyPair, PublicKey}
import akka.actor.ActorSelection
import mvp2.messages.{Get, MyPublicKey, PeerPublicKey}
import mvp2.utils.{ECDSA, EncodingUtils}

class KeyKeeper extends CommonActor {

  val myKeys: KeyPair = ECDSA.createKeyPair

  var nodesKeys: Set[PublicKey] = Set.empty

  val networker: ActorSelection = context.actorSelection("/user/starter/blockchainer/networker")

  override def preStart(): Unit = {
    logger.info(s"My public key is: ${EncodingUtils.encode2Base64(ECDSA.compressPublicKey(myKeys.getPublic))}")
    networker ! MyPublicKey(myKeys.getPublic)
  }

  override def specialBehavior: Receive = {
    case Get => sender ! myKeys.getPublic
    case PeerPublicKey(key) =>
      logger.info(s"Get key from remote: ${EncodingUtils.encode2Base64(ECDSA.compressPublicKey(key))}")
      nodesKeys += key
  }
}
