package mvp2.Actors

import java.net.InetSocketAddress

import akka.util.ByteString
import mvp2.Messages._

class Informator extends CommonActor {

  var actualBlockchainInfo: CurrentBlockchainInfo = CurrentBlockchainInfo(0, None, None)
  var actualNetworkerInfo: CurrentNetworkerInfo = CurrentNetworkerInfo(List.empty[InetSocketAddress])
  var actualAccountantInfo: CurrentAccountantInfo = CurrentAccountantInfo(Map.empty[ByteString,Account])

  override def preStart(): Unit = {
    logger.info("Starting the Informator!")
  }

  override def specialBehavior: Receive = {
    case Get => sender ! actualBlockchainInfo
    case GetNetworkerInfo => sender ! actualNetworkerInfo
    case GetAccountantInfo => sender ! actualAccountantInfo

    case currentBlockchainInfo: CurrentBlockchainInfo => actualBlockchainInfo = currentBlockchainInfo
    case currentNetworkerInfo: CurrentNetworkerInfo => actualNetworkerInfo = currentNetworkerInfo
    case currentAccountantInfo: CurrentAccountantInfo => actualAccountantInfo = currentAccountantInfo


  }
}