package mvp2.Utils

case class Settings(port: Int,
                    otherNodes: List[Node],
                    heartbeat: Int,
                    apiSettings: ApiSettings,
                    levelDBSettings: LevelDBSettings)

case class Node(host: String, port: Int)

case class ApiSettings(httpHost: String, httpPort: Int, timeout: Int)

case class LevelDBSettings(enableRestore: Boolean)