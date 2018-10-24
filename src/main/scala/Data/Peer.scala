package Data

import java.net.InetSocketAddress

import Data.BlockchainCompareStatus.CompareStatus

case class Peer(remoteAddress: InetSocketAddress,
                blockchainCompareStatus: CompareStatus,
                lastPollingTime: Long)

object BlockchainCompareStatus {

  trait CompareStatus

  case object Equal extends CompareStatus

  case object Older extends CompareStatus

  case object Younger extends CompareStatus

  case object Unknown extends CompareStatus
}