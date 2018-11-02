package mvp2.actors

import java.net.InetAddress
import akka.actor.ActorSelection
import mvp2.messages.TimeDelta
import mvp2.utils.NetworkTimeProviderSettings
import org.apache.commons.net.ntp.{NTPUDPClient, TimeInfo}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

class TimeProvider(ntpSettings: NetworkTimeProviderSettings) extends CommonActor {

  val client: NTPUDPClient = new NTPUDPClient()
  client.setDefaultTimeout(ntpSettings.timeout.toMillis.toInt)

  val actors: Seq[ActorSelection] = Seq(
    context.actorSelection("/user/starter/blockchainer"),
    context.actorSelection("/user/starter/blockchainer/publisher")
  )

  override def preStart(): Unit =
    context.system.scheduler.schedule(1 seconds, ntpSettings.updateEvery)(sendTimeToActors)

  override def specialBehavior: Receive = {
    case _ =>
  }

  def updateOffSetTry: Try[Long] = Try {
      client.open()
      val info: TimeInfo = client.getTime(InetAddress.getByName(ntpSettings.server))
      client.close()
      info.computeDetails()
      info.getOffset
    }

  def sendTimeToActors: Unit = updateOffSetTry.foreach(offset => actors.foreach(ref => ref ! TimeDelta(offset)))
}
