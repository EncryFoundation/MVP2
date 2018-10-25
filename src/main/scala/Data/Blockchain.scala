package Data

import akka.util.ByteString

import scala.collection.immutable.HashMap

sealed trait Chain {
  var chain: HashMap[Int, Block]

  def size: Int = chain.size

  def lastBlock: Block = chain.last._2

  def update(block: Block): HashMap[Int, Block] = {
    chain.updated(block.height, block)
  }
}

case object Blockchain extends Chain {

  override var chain: HashMap[Int, Block] = HashMap.empty
  var lastKeyBlockVar: KeyBlock = KeyBlock(-1, ByteString.empty, ByteString.empty)
  var lastMicroBlockVar: MicroBlock =
    MicroBlock(-1, ByteString.empty, ByteString.empty, List(), ByteString.empty)

  def genesisBlock: KeyBlock = ???

  override def update(block: Block): HashMap[Int, Block] = {
    block match {
      case keyBlock: KeyBlock => lastKeyBlockVar = keyBlock
      case microBlock: MicroBlock => lastMicroBlockVar = microBlock
    }
    chain.updated(block.height, block)
  }

  def getLastEpoch: HashMap[Int, Block] = {
    val a = chain.dropWhile(x => x._1 != lastKeyBlockVar.height)
    a.foreach(x => println(x._1))
    a
  }
}

final case class Appendix(override var chain: HashMap[Int, Block]) extends Chain {

  override def size: Int = chain.size

  override def lastBlock: Block = chain.last._2

  override def update(block: Block): HashMap[Int, Block] = ???
}