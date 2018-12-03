package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import mvp2.data.InnerMessages._
import mvp2.data.NetworkMessages._
import scala.concurrent.ExecutionContext.Implicits.global
import mvp2.utils.{EncodingUtils, Settings}
import org.influxdb.{InfluxDB, InfluxDBFactory}
import scala.concurrent.duration._
import scala.language.postfixOps

class InfluxActor(settings: Settings) extends CommonActor {

  val myNodeAddress: String = InetAddress.getLocalHost.getHostAddress

  var currentDelta: Long = 0

  var msgFromRemote: Map[InetSocketAddress, Map[String, Int]] = Map.empty

  var msgToRemote: Map[InetSocketAddress, Map[String, Int]] = Map.empty

  val influxDB: InfluxDB =
    InfluxDBFactory.connect(
      settings.influx.host,
      settings.influx.login,
      settings.influx.password
    )

  override def preStart(): Unit = {
    logger.info("Start influx actor")
    influxDB.write(settings.influx.port, s"""startMvp value=12""")
    context.system.scheduler.schedule(1.seconds,
      settings.testingSettings.iteratorsSyncTime.millisecond)(syncIterators())
  }

  def getMsgIncrements(remote: InetSocketAddress,
                       msg: String,
                       iterators: Map[InetSocketAddress, Map[String, Int]]):
  (Map[InetSocketAddress, Map[String, Int]], Int) = {
    msgFromRemote.find(_._1 == remote) match {
      case Some(msgInfo) => msgInfo._2.find(_._1 == msg) match {
        case Some(i) =>
          (msgFromRemote - msgInfo._1 + msgInfo.copy(_2 = msgInfo._2 - msg + (msg -> (i._2 + 1))), i._2 + 1)
        case None =>
          (msgFromRemote - msgInfo._1 + msgInfo.copy(_2 = msgInfo._2 + (msg -> 1)), 1)
      }
      case None =>
        (msgFromRemote + (remote -> Map(msg -> 1)), 1)
    }
  }

  override def specialBehavior: Receive = {
    case FromNet(message, remote, id) =>
      val (newIncrements, i) = getMsgIncrements(remote, message.name, msgFromRemote)
      msgFromRemote = newIncrements
      influxDB.write(settings.influx.port,
        s"""networkMsg,node=$myNodeAddress,msgid=${EncodingUtils.encode2Base16(id) + i},msg=${message.name} value=$time""")
//      logger.info(s"Report about msg:${EncodingUtils.encode2Base16(id)} with incr: $i")
    case ToNet(message, remote, id) =>
      val (newIncrements, i) = getMsgIncrements(remote, message.name, msgToRemote)
      msgToRemote = newIncrements
      influxDB.write(settings.influx.port,
        s"""networkMsg,node=$myNodeAddress,msgid=${EncodingUtils.encode2Base16(id) + i},msg=${message.name} value=$time""")
//      logger.info(s"Sent data about message to influx: $message with id: ${EncodingUtils.encode2Base16(id)} with incr: $i")
    case SyncMessageIteratorsFromRemote(iterators, remote) =>
//      logger.info(s"Sync iterators from $remote")
      msgFromRemote = msgFromRemote - remote + (remote -> iterators)
    case TimeDelta(delta: Long) =>
//      logger.info(s"Update delta to: $delta")
      currentDelta = delta
    case _ =>
  }

  def syncIterators(): Unit =
    msgToRemote.foreach {
      case (peer, iterators) => context.actorSelection("/user/starter/blockchainer/networker/udpSender") !
        ToNet(SyncMessageIterators(iterators.map(iter => IterInfo(iter._1, iter._2)).toList), peer)
    }

  def time: Long = System.currentTimeMillis() + currentDelta
}