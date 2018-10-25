package Data

import scala.collection.immutable.HashMap

sealed trait Chain {
  val chain: HashMap[Int, Block]

  def size: Int = chain.size

  def lastBlock: Block = chain.last._2

  def update(block: Block): HashMap[Int, Block]
}

case object Blockchain extends Chain {

  override val chain: HashMap[Int, Block] = HashMap.empty

  def lastKeyBlock: KeyBlock = ???

  def lastMicroBlock: MicroBlock = ???

  def genesysBlock: KeyBlock = ???

  def update(block: Block): HashMap[Int, Block] = ???

}

final abstract case class Appendix(chain: HashMap[Int, Block]) extends Chain
