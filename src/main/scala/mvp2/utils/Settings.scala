package mvp2.utils

case class Settings(port: Int,
                    otherNodes: List[Node],
                    heartbeat: Int,
                    plannerHeartbeat: Int,
                    blockPeriod: Long,
                    biasForBlockPeriod: Long,
                    newBlockchain: Boolean,
                    downloadStateFrom: Option[String] = None,
                    apiSettings: ApiSettings,
                    ntp: NetworkTimeProviderSettings,
                    influx: InfluxSettings,
                    testingSettings: TestingSettings,
                    mempoolSetting: MempoolSetting
                   )

case class Node(host: String, port: Int)

case class ApiSettings(httpHost: String, httpPort: Int, timeout: Int, enableStateDownload: Boolean)

case class InfluxSettings(host: String, port: Int, login: String, password: String)

case class NetworkTimeProviderSettings(server: String, updateEvery: Int, timeout: Int)

case class MempoolSetting(transactionsValidTime: Long, mempoolCleaningTime: Long)

case class TestingSettings(messagesTime: Boolean, iteratorsSyncTime: Int)