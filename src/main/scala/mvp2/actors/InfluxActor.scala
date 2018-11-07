package mvp2.actors

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import mvp2.messages._
import mvp2.utils.Settings
import org.influxdb.{InfluxDB, InfluxDBFactory}

class InfluxActor(settings: Settings) extends Actor with StrictLogging {

  val myNodeAddress: String = InetAddress.getLocalHost.getHostAddress

  var pingPongResponsePequestTime: Map[InetSocketAddress, Long] = Map.empty

  val influxDB: InfluxDB = InfluxDBFactory.connect(
    settings.influx.map(infl => infl.host).getOrElse(""),
    settings.influx.map(infl => infl.login).getOrElse(""),
    settings.influx.map(infl => infl.password).getOrElse(""),
  )

  val nodeName: String = settings.influx.flatMap(infl => infl.nodeName) match {
    case Some(value) => value
    case None => InetAddress.getLocalHost.getHostAddress + ":" + settings.port
  }

  val port: Int = settings.influx.map(infl => infl.port).getOrElse(0)

  override def preStart(): Unit = {
    influxDB.write(port, s"""startMvp value="$nodeName"""")
  }

  override def receive: Receive = {
    case MessageFromRemote(message, remote) =>
      val msg: String = message match {
        case Ping => "ping"
        case Pong =>
          pingPongResponsePequestTime.get(remote).foreach { pingSendTime =>
            influxDB.write(port,
              s"""pingPongResponseTime,remote="$myNodeAddress" value=${System.currentTimeMillis() - pingSendTime},node="${remote.getAddress}"""".stripMargin)
          }
          pingPongResponsePequestTime = pingPongResponsePequestTime - remote
          "pong"
        case Peers(_, _) => "peers"
      }
      influxDB.write(port,
        s"""msgFromRemote,node="$myNodeAddress",remote="${remote.getAddress}" value=$msg""")
    case SendToNetwork(message, remote) =>
      val msg: String = message match {
        case Ping =>
          pingPongResponsePequestTime = pingPongResponsePequestTime + ((remote, System.currentTimeMillis()))
          "ping"
        case Pong => "pong"
        case Peers(_, _) => "peers"
      }
      influxDB.write(port,
        s"""msgToRemote,node=$myNodeAddress value="$msg",remote="${remote.getAddress.getHostAddress}"""")
    case _ =>
  }
}