package mvp2.actors

import java.net.InetAddress
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import mvp2.messages._
import mvp2.utils.InfluxSettings
import org.influxdb.{InfluxDB, InfluxDBFactory}

class InfluxActor(settings: InfluxSettings) extends Actor with StrictLogging {

  val myNodeAddress: String = InetAddress.getLocalHost.getHostAddress

  val influxDB: InfluxDB = InfluxDBFactory.connect(
    settings.host,
    settings.login,
    settings.password
  )

  override def preStart(): Unit = {
    logger.info("Starting Influx actor")
    influxDB.write(settings.port, s"""startMvp value=12""")
  }

  override def receive: Receive = {
    case MessageFromRemote(message, remote) =>
      val msg: String = message match {
        case Ping => "ping"
        case Pong => "pong"
        case Peers(_, _) => "peers"
      }
      influxDB.write(settings.port,
        s"msgFromRemote,node=$myNodeAddress msg=$msg,remote=${remote.getAddress}")
    case SendToNetwork(message, remote) =>
      val msg: String = message match {
        case Ping => "ping"
        case Pong => "pong"
        case Peers(_, _) => "peers"
      }
      println("Send to remote")
      influxDB.write(settings.port,
        s"msgToRemote,node=$myNodeAddress msg=$msg,remote=${remote.getAddress}")
    case _ =>
  }
}