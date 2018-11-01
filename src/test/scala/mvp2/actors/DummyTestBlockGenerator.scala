package mvp2.actors

import java.nio.charset.StandardCharsets
import akka.util.ByteString
import mvp2.data.{Block, KeyBlock, MicroBlock, Transaction}
import mvp2.utils.Sha256
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer

object DummyTestBlockGenerator {

  var addressNoncePull: ListBuffer[(ByteString, Int)] = ListBuffer.range(0, 10)
    .map(_ => (generateByteString, 0))

  def generateKeyBlock(prevHash: ByteString, prevHeight: Int): KeyBlock =
    KeyBlock(prevHeight + 1, System.currentTimeMillis, prevHash, generateTenTransactions, ByteString.empty)

  def generateMicroBlock(prevHash: ByteString, prevMicroHash: ByteString, currentBlockHash: ByteString,
                         prevHeight: Int): MicroBlock =
    MicroBlock(prevHeight + 1, System.currentTimeMillis, prevHash, prevMicroHash, currentBlockHash,
      generateTenTransactions, ByteString.empty)

  def generateTenTransactions: List[Transaction] = List.range(0, 10).map(e => generateTransaction(e))

  def generateTransaction(index: Int): Transaction = {
    val addressNonce = addressNoncePull(index)
    addressNoncePull(index) = (addressNoncePull(index)._1, addressNoncePull(index)._2 + 1)
    Transaction(addressNonce._1, addressNonce._2 + 1, generateHash, generateByteString)
  }

  def generateChain(length: Int): List[Block] = List.range(1, length + 1)
    .map(e => generateKeyBlock(generateByteString, e))

  def generateByteString: ByteString = ByteString(java.util.UUID.randomUUID()
    .toString.getBytes(StandardCharsets.UTF_8))

  def generateHash: ByteString = Sha256.toSha256(java.util.UUID.randomUUID().toString)

  def generateValidChain: HashMap[Long, Block] = {
    val firstKeyBlock: KeyBlock =
      KeyBlock(0L, System.currentTimeMillis(), ByteString.empty, List(), generateByteString)
    val firstMicroBlock: MicroBlock =
      MicroBlock(
        1,
        System.currentTimeMillis(),
        firstKeyBlock.data,
        ByteString.empty,
        generateHash,
        List(),
        generateByteString
      )
    val listFiveMicroBlocks: List[MicroBlock] = List.range(1, 5).foldLeft(List(firstMicroBlock)) { case (cur, prev) =>
      val nextMicro =
        MicroBlock(
          cur.last.height + 1,
          System.currentTimeMillis(),
          firstKeyBlock.data,
          cur.last.data,
          generateHash,
          List(),
          generateByteString
        )
      cur :+ nextMicro
    }
    val newKeyBlock: KeyBlock =
      KeyBlock(
        listFiveMicroBlocks.last.height + 1,
        System.currentTimeMillis(),
        firstKeyBlock.data, List(),
        generateByteString
      )
    val notCompletedChain: HashMap[Long, Block] = listFiveMicroBlocks.foldLeft(HashMap[Long, Block]()) {
      case (next, prev) => next + (prev.height -> prev)
    }
    val completedChain: HashMap[Long, Block] =
      notCompletedChain + (newKeyBlock.height -> newKeyBlock) + (firstKeyBlock.height -> firstKeyBlock)
    completedChain
  }
}