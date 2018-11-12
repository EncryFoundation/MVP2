package mvp2.utils

case class Settings(port: Int,
                    otherNodes: List[Node],
                    heartbeat: Int,
                    plannerHeartbeat: Int,
                    blockPeriod: Long,
                    biasForBlockPeriod: Long,
                    newBlockchain: Boolean,
                    apiSettings: ApiSettings,
                    ethereumSettings: EthereumSettings,
                    influx: Option[InfluxSettings],
                    testingSettings: Option[TestingSettings]
                   )

case class Node(host: String, port: Int)

case class ApiSettings(httpHost: String, httpPort: Int, timeout: Int)

case class InfluxSettings(host: String, port: Int, login: String, password: String)

case class EthereumSettings(userAccount: String, userPassword: String, receiverAccount:String,
                            peerRPCAddress: String, gasPrice: Long)

case class TestingSettings(pingPong: Boolean)