package mvp2.data

sealed trait Chain {
  var chain: List[KeyBlock]

  def size: Int = chain.size

  def lastBlock: Block = chain.last
}

final case class Blockchain (var chain: List[KeyBlock] = List.empty) extends Chain {

  val maxHeight: Long = chain.lastOption.map(_.height).getOrElse(-1)

  def + (block: KeyBlock): Blockchain = this.copy(chain :+ block)

  def isApplicable(block: KeyBlock): Boolean =
    if (chain.isEmpty && block.height == 0) true
    else chain.lastOption.exists(lastBlock => block.height == lastBlock.height + 1)

  def getMissingPart(remoteHeight: Long): Option[List[KeyBlock]] =
    if (chain.lastOption.exists(_.height == remoteHeight)) None
    else Some(chain.drop(remoteHeight.toInt))
}