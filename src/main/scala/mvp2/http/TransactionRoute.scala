package mvp2.http

import akka.actor.{ActorRefFactory, ActorSelection}
import akka.http.scaladsl.server.Route
import mvp2.utils.Settings
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import mvp2.data.Transaction
import mvp2.MVP2.system

case class TransactionRoute(settings: Settings, implicit val context: ActorRefFactory) {

  val publisher: ActorSelection = system.actorSelection("user/starter/blockchainer/publisher")

  val route: Route = {
    path("getTxs")(entity(as[List[Transaction]]) {
      txs => complete {
          txs.foreach(tx => publisher ! tx)
          StatusCodes.OK
        }
      })
  }

}