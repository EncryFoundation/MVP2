package Actors

import Actors.InfluxActor.TestMessage
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

  override def preStart(): Unit = {
    logger.info("Start Influx actor")
    influxDB.write(influxPort, s"""startMvp value=1""")
  }

  override def receive: Receive = {
    case TestMessage => logger.info("Got test message")
    case _ =>
  }

}

object InfluxActor {

  case object TestMessage

}