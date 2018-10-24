package Actors

import Actors.InfluxActor.FirstMessage
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import org.influxdb.{InfluxDB, InfluxDBFactory}

class InfluxActor extends Actor with StrictLogging {

  val influxPort: Int = 0

  val influxDB: InfluxDB = InfluxDBFactory.connect(
    "",
    "",
    ""
  )

  override def preStart(): Unit = influxDB.write(influxPort, s"""startMVP2 value=${}""")

  override def receive: Receive = {
    case FirstMessage(message) =>
      logger.info(s"Got first message: $message")
      influxDB.write(influxPort, s"""firstMessage value=${}""")
    case _ =>
  }

}

object InfluxActor {

  case class FirstMessage(message: String)

}