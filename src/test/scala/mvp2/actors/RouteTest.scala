package mvp2.actors

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import akka.http.scaladsl.model.MessageEntity
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.scalatest.PropSpecLike
import mvp2.http.Routes
import mvp2.utils.EncodingUtils._
import scala.language.postfixOps
import mvp2.data.Transaction
import mvp2.utils.Settings
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.Json
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Matchers
import io.circe.generic.auto._
import io.circe.syntax._

class RouteTest
  extends PropSpecLike
    with ScalatestRouteTest
    with ScalaFutures
    with Matchers
    with FailFastCirceSupport {

  val settings: Settings = ConfigFactory.load("local.conf").withFallback(ConfigFactory.load)
    .as[Settings]("mvp")

  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(settings.apiSettings.timeout.second)

  lazy val routesTest: Route = Routes(settings, system).getTxs

  property("Tx route should return StatusCodes.Ok") {
    val listOfTransactions: List[Json] = List(
      Transaction(ByteString.empty, 0L, 0L, ByteString.empty, ByteString.empty).asJson,
      Transaction(ByteString.empty, 0L, 0L, ByteString.empty, ByteString.empty).asJson,
      Transaction(ByteString.empty, 0L, 0L, ByteString.empty, ByteString.empty).asJson
    )
    val txsEntity = Marshal(listOfTransactions).to[MessageEntity].futureValue
    val request = Post("/sendTxs").withEntity(txsEntity)

    request ~> routesTest ~> check {
      status should ===(StatusCodes.OK)
    }
  }
}