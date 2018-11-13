package mvp2.utils

import scala.concurrent.duration.FiniteDuration

case class Settings(port: Int,
                    otherNodes: List[Node],
                    heartbeat: Int,
                    plannerHeartbeat: Int,
                    blockPeriod: Long,
                    biasForBlockPeriod: Long,
                    newBlockchain: Boolean,
                    apiSettings: ApiSettings,
                    ntp: NetworkTimeProviderSettings,
                    influx: Option[InfluxSettings],
                    testingSettings: Option[TestingSettings]
                   )

case class Node(host: String, port: Int)

case class ApiSettings(httpHost: String, httpPort: Int, timeout: Int)

case class InfluxSettings(host: String, port: Int, login: String, password: String)

case class NetworkTimeProviderSettings(server: String, updateEvery: FiniteDuration, timeout: FiniteDuration)

case class TestingSettings(pingPong: Boolean)