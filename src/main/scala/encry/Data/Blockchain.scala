package encry.Data


sealed trait Chain {
  val chain: List[Block]
  def size: Int = chain.length
  def lastBlock: Block = chain.last
}
final case class Blockchain () {
  def lastGeneralBlock: GeneralBlock = ???
}
final case class Appendix(chain: List[Block]){
}
