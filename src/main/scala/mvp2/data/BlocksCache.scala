package mvp2.data

case class BlocksCache(blocks: List[KeyBlock] = List.empty) {

  def isEmpty: Boolean = blocks.isEmpty

  def + (block: KeyBlock): BlocksCache =
    this.copy((blocks :+ block).sortWith((block1, block2) => block1.height > block2.height))

  def + (blocksToAdd: Seq[KeyBlock]): BlocksCache =
    this.copy((blocks ++ blocksToAdd).sortWith((block1, block2) => block1.height > block2.height))

  def - (block: KeyBlock): BlocksCache = this.copy(blocks.filter(_.height != block.height))

  def getApplicableBlock(blochchain: Blockchain): Option[KeyBlock] =
    if (blocks.lastOption.exists(blochchain.isApplicable)) Some(blocks.last)
    else None
}
