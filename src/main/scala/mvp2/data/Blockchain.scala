package mvp2.data

sealed trait Chain {
  var chain: List[KeyBlock]

  def size: Int = chain.size

  def lastBlock: Block = chain.last
}

final case class Blockchain (var chain: List[KeyBlock] = List.empty) extends Chain