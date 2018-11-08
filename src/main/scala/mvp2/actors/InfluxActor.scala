package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import mvp2.messages._
import mvp2.utils.{EncodingUtils, InfluxSettings}
import org.influxdb.{InfluxDB, InfluxDBFactory}

class InfluxActor(settings: InfluxSettings) extends Actor with StrictLogging {

  val myNodeAddress: String = InetAddress.getLocalHost.getHostAddress

  var pingPongResponsePequestTime: Map[InetSocketAddress, Long] = Map.empty

  var msgFromRemote: Map[InetSocketAddress, Map[String, Int]] = Map.empty

  var msgToRemote: Map[InetSocketAddress, Map[String, Int]] = Map.empty

  val influxDB: InfluxDB = InfluxDBFactory.connect(
    settings.host,
    settings.login,
    settings.password
  )

  override def preStart(): Unit = {
    influxDB.write(settings.port, s"""startMvp value=12""")
  }

  def getFromRemoteMsgIncrement(remote: InetSocketAddress, msg: String): Int =
    msgFromRemote.find(_._1 == remote) match {
      case Some(msgInfo) => msgInfo._2.find(_._1 == msg) match {
        case Some(i) =>
          msgFromRemote = msgFromRemote - msgInfo._1 + msgInfo.copy(_2 = msgInfo._2 - msg + (msg -> (i._2 + 1)))
          i._2 + 1
        case None =>
          msgFromRemote = msgFromRemote - msgInfo._1 + msgInfo.copy(_2 = msgInfo._2 + (msg -> 1))
          1
      }
      case None =>
        msgFromRemote += (remote -> Map(msg -> 1))
        1
    }

  def getToRemoteMsgIncrement(remote: InetSocketAddress, msg: String): Int =
    msgToRemote.find(_._1 == remote) match {
      case Some(msgInfo) => msgInfo._2.find(_._1 == msg) match {
        case Some(i) =>
          msgToRemote = msgToRemote - msgInfo._1 + msgInfo.copy(_2 = msgInfo._2 - msg + (msg -> (i._2 + 1)))
          i._2 + 1
        case None =>
          msgToRemote = msgToRemote - msgInfo._1 + msgInfo.copy(_2 = msgInfo._2 + (msg -> 1))
          1
      }
      case None =>
        msgToRemote += (remote -> Map(msg -> 1))
        1
    }

  override def receive: Receive = {
    case MsgFromNetwork(message, id, remote) =>
      val msg: String = message match {
        case Ping => "ping"
        case Pong =>
          pingPongResponsePequestTime.get(remote).foreach { pingSendTime =>
            influxDB.write(settings.port,
              s"""pingPongResponseTime,remote="$myNodeAddress" value=${System.currentTimeMillis() - pingSendTime},node="${remote.getAddress}"""".stripMargin)
          }
          pingPongResponsePequestTime = pingPongResponsePequestTime - remote
          "pong"
        case Peers(_, _) => "peers"
        case Blocks(_) => "blocks"
      }
      val i: Int = getFromRemoteMsgIncrement(remote, msg)
      influxDB.write(settings.port,
        s"""msgFromRemote,node="$myNodeAddress",remote="${remote.getAddress}" value=$msg""")
      influxDB.write(settings.port,
        s"""networkMsg,node=$myNodeAddress,msgid=${EncodingUtils.encode2Base16(id) + i},msg=$msg value=${System.currentTimeMillis()}""")
      logger.info(s"Report about msg: ${EncodingUtils.encode2Base16(id)} with incr: $i")
    case MsgToNetwork(message, id, remote) =>
      val msg: String = message match {
        case Ping =>
          pingPongResponsePequestTime = pingPongResponsePequestTime + ((remote, System.currentTimeMillis()))
          "ping"
        case Pong => "pong"
        case Peers(_, _) => "peers"
        case Blocks(_) => "blocks"
      }
      val i: Int = getToRemoteMsgIncrement(remote, msg)
      influxDB.write(settings.port,
        s"""msgToRemote,node=$myNodeAddress value="$msg",remote="${remote.getAddress.getHostAddress}"""")
      influxDB.write(settings.port,
        s"""networkMsg,node=$myNodeAddress,msgid=${EncodingUtils.encode2Base16(id) + i},msg=$msg value=${System.currentTimeMillis()}""")
      logger.info(s"Send: $message with id: ${EncodingUtils.encode2Base16(id)} with incr: $i")
    case _ =>
  }
}