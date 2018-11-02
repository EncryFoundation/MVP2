package mvp2.data

import akka.util.ByteString
import com.google.common.primitives.Longs
import mvp2.utils.Sha256

case class Transaction(publicKey: ByteString,
                       nonce: Long,
                       signature: ByteString,
                       data: ByteString) {

  def getHash: ByteString =
    Sha256.toSha256(
      publicKey ++
      ByteString(Longs.toByteArray(nonce)) ++
      signature ++
      data
    )

  def isValid: Boolean = true //FIXME change in later version
}
