package mvp2.utils

import akka.util.ByteString

object BlockchainUtils {

  def merkleTree(txHashes: Seq[ByteString]): ByteString = {
    val possibleRoot = txHashes
      .sliding(2)
      .toSeq
      .map(txs => Sha256.toSha256(txs.foldLeft(ByteString.empty)(_ ++ _)))
    if (possibleRoot.size > 1) merkleTree(possibleRoot)
    else if (possibleRoot.isEmpty) ByteString.empty
    else possibleRoot.head
  }
}
