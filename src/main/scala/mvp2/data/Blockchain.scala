package mvp2.data

import scala.collection.immutable.TreeMap

sealed trait Chain {
  var chain: TreeMap[Long, Block]

  def size: Int = chain.size

  def lastBlock: Block = chain.last._2

  def update(block: Block): Unit = chain = chain.updated(block.height, block)
}

final case class Blockchain (var chain: List[KeyBlock] = List.empty) {

  def + (block: KeyBlock): Blockchain = this.copy(chain :+ block)

  def isApplicable(block: KeyBlock): Boolean = block.height == chain.head.height + 1

  def getMissingPart(remoteHeight: Int): Option[List[KeyBlock]] =
    if (remoteHeight == chain.last.height) None
    else Some(chain.drop(remoteHeight))
}

final case class Appendix(override var chain: TreeMap[Long, Block]) extends Chain {

  override def size: Int = chain.size

  override def lastBlock: Block = chain.last._2

  override def update(block: Block): Unit = chain = chain.updated(block.height, block)

}