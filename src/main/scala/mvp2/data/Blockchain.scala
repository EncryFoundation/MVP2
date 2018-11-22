package mvp2.data

import scala.collection.immutable.SortedSet

sealed trait Chain {
  var chain: SortedSet[KeyBlock]

  def size: Int = chain.size

  def lastBlock: Block = chain.last
}

final case class Blockchain (var chain: SortedSet[KeyBlock] = SortedSet.empty[KeyBlock]) extends Chain {

  val maxHeight: Long = chain.lastOption.map(_.height).getOrElse(-1)

  def + (block: KeyBlock): Blockchain = this.copy(chain + block)

  def isApplicable(block: KeyBlock): Boolean =
    if (chain.isEmpty && block.height == 0) true
    else chain.lastOption.exists(lastBlock => block.height == lastBlock.height + 1)

  def getMissingPart(remoteHeight: Long): Option[SortedSet[KeyBlock]] =
    if (chain.lastOption.exists(_.height == remoteHeight)) None
    else Some(chain.drop(remoteHeight.toInt + 1))
}

final case class BlocksCache(var chain: SortedSet[KeyBlock] = SortedSet.empty[KeyBlock]) extends Chain {

  def isEmpty: Boolean = chain.isEmpty

  def + (block: KeyBlock): BlocksCache =
    this.copy(chain + block)

  def + (blocksToAdd: Seq[KeyBlock]): BlocksCache =
    this.copy(chain ++ blocksToAdd)

  def - (block: KeyBlock): BlocksCache = this.copy(chain.filter(_.height != block.height))

  def getApplicableBlock(blochchain: Blockchain): Option[KeyBlock] =
    if (chain.headOption.exists(blochchain.isApplicable)) Some(chain.head)
    else None
}
