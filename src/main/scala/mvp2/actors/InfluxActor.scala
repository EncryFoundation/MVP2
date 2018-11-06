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

  val influxDB: InfluxDB = InfluxDBFactory.connect(
    settings.host,
    settings.login,
    settings.password
  )

  override def preStart(): Unit = {
    influxDB.write(settings.port, s"""startMvp value=12""")
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
      influxDB.write(settings.port,
        s"""msgFromRemote,node="$myNodeAddress",remote="${remote.getAddress}" value=$msg""")
      influxDB.write(settings.port,
        s"""networkMsg,node=$myNodeAddress value=${System.currentTimeMillis()},msgid="${EncodingUtils.encode2Base64(id)}"""")
    case MsgToNetwork(message, id, remote) =>
      val msg: String = message match {
        case Ping =>
          pingPongResponsePequestTime = pingPongResponsePequestTime + ((remote, System.currentTimeMillis()))
          "ping"
        case Pong => "pong"
        case Peers(_, _) => "peers"
        case Blocks(_) => "blocks"
      }
      influxDB.write(settings.port,
        s"""msgToRemote,node=$myNodeAddress value="$msg",remote="${remote.getAddress.getHostAddress}"""")
      influxDB.write(settings.port,
        s"""networkMsg,node=$myNodeAddress value=${System.currentTimeMillis()},msgid="${EncodingUtils.encode2Base64(id)}"""")
    case _ =>
  }
}