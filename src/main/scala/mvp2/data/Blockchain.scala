package mvp2.data

import scala.collection.immutable.TreeMap

sealed trait Chain {
  var chain: TreeMap[Long, Block]

  def size: Int = chain.size

  def lastBlock: Block = chain.last._2

  def update(block: Block): Unit = chain = chain.updated(block.height, block)
}

trait Blockchain extends Chain {

  override var chain: TreeMap[Long, Block] = TreeMap.empty

  def lastKeyBlock: Option[Block] = chain.lastOption.map(_._2)

  def lastMicroBlock: MicroBlock = ???

  def genesisBlock: KeyBlock = ???

  def update(newChainPart: TreeMap[Long, Block]): Unit = chain ++= newChainPart

}

final case class Appendix(override var chain: TreeMap[Long, Block]) extends Chain {

  override def size: Int = chain.size

  override def lastBlock: Block = chain.last._2

  override def update(block: Block): Unit = chain = chain.updated(block.height, block)

}