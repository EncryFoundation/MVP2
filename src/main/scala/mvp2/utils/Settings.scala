package mvp2.utils

case class Settings(port: Int,
                    otherNodes: List[Node],
                    heartbeat: Int,
                    apiSettings: ApiSettings,
                    influx: InfluxDB
                   )

case class Node(host: String, port: Int)

case class ApiSettings(httpHost: String, httpPort: Int, timeout: Int)

case class InfluxDB(host: String, port: Int, login: String, password: String)