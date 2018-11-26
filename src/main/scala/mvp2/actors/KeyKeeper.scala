package mvp2.actors

import java.security.{KeyPair, PublicKey}
import mvp2.data.InnerMessages.{Get, MyPublicKey, PeerPublicKey}
import mvp2.utils.{ECDSA, EncodingUtils}

class KeyKeeper extends CommonActor {

  val myKeys: KeyPair = ECDSA.createKeyPair

  var allPublicKeys: Set[PublicKey] = Set.empty[PublicKey]

  override def preStart(): Unit = {
    println(s"My public key issss: ${EncodingUtils.encode2Base16(ECDSA.compressPublicKey(myKeys.getPublic))}")
    val pubKeyMessage: MyPublicKey =  MyPublicKey(ECDSA.compressPublicKey(myKeys.getPublic))
    context.actorSelection("/user/starter/blockchainer/networker") ! pubKeyMessage
    context.actorSelection("/user/starter/blockchainer/planner") ! pubKeyMessage
    context.actorSelection("/user/starter/blockchainer/publisher") ! myKeys
  }

  override def specialBehavior: Receive = {
    case Get => sender ! myKeys.getPublic
    case PeerPublicKey(key) =>
      logger.info(s"Got public key from remote: ${EncodingUtils.encode2Base16(key)} on KeyKeeper.")
      allPublicKeys = allPublicKeys + ECDSA.uncompressPublicKey(key)
  }
}