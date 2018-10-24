package encry.Actors

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
    logger.info("Starting Influx actor")
    influxDB.write(influxPort, s"""startMvp value=12""")
  }

  override def receive: Receive = {
    case _ =>
  }

}