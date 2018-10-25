package Actors

import java.nio.charset.StandardCharsets

import Data.{Block, GeneralBlock, MicroBlock, Transaction}
import akka.util.ByteString
import utils.Sha256

object DummyTestBlockGenerator {

  var addressNoncePull: Map[Int, (ByteString, Int)] = List.range(1,10).map(_ => generateByteString)
    .zip(List.fill(10)(0)).zipWithIndex.map(_.swap).toMap

  def generateKeyBlock(prevHash: ByteString, prevHeight: Int): GeneralBlock =
    GeneralBlock(prevHeight+1, prevHash, generateTenTransactions, ByteString.empty)

  def generateMicroBlock(prevHash: ByteString, prevMicroHash: ByteString, prevHeight: Int): MicroBlock =
    MicroBlock(prevHeight+1, prevHash, prevMicroHash, generateTenTransactions, ByteString.empty)

  def generateTenTransactions: List[Transaction] = List.range(1,10).map(_ => generateTransaction)

  def generateTransaction: Transaction = Transaction()

  def generateChain(length:Int): List[Block] = List.range(1,10)
    .map(e => generateKeyBlock(generateByteString,e))


  def generateByteString: ByteString = ByteString(java.util.UUID.randomUUID()
    .toString.getBytes(StandardCharsets.UTF_8))

  def generateHash: ByteString = Sha256.toSha256(java.util.UUID.randomUUID().toString)
}
