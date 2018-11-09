package mvp2.utils

object EthRequestType extends Enumeration {
  type EthRequestType = Value
  val UNLOCKACC, SENDTX, GETRESULT = Value
}
