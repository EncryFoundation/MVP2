package mvp2.data

case class BlocksCache(blocks: List[KeyBlock] = List.empty) {

  def + (block: KeyBlock): BlocksCache = this.copy((blocks :+ block).sortWith(_.height > _.height))

  def - (block: KeyBlock): BlocksCache = this.copy(blocks.filter(_.height == block.height))

  def getApplicableBlock(blochchain: Blockchain): Option[KeyBlock] =
    if (blochchain.isApplicable(blocks.head)) Some(blocks.head)
    else None
}
