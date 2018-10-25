package Data

import scala.collection.immutable.HashMap

sealed trait Chain {
  var chain: HashMap[Int, Block]

  def size: Int = chain.size

  def lastBlock: Block = chain.last._2

  def update(block: Block): HashMap[Int, Block] = {chain.updated(block.height,block)}
}

case object Blockchain extends Chain {

  override var chain: HashMap[Int, Block] = HashMap.empty

  def lastKeyBlock: KeyBlock = ???

  def lastMicroBlock: MicroBlock = ???

  def genesysBlock: KeyBlock = ???

  override def update(block: Block): HashMap[Int, Block] = ???

}

final case class Appendix(override var chain: HashMap[Int, Block]) extends Chain {

  override def size: Int = chain.size

  override def lastBlock: Block = chain.last._2

  override def update(block: Block): HashMap[Int, Block] = ???
}
