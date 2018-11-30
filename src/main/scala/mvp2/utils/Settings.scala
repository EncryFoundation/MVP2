package mvp2.utils

case class Settings(port: Int,
                    otherNodes: List[Node],
                    network: NetworkSettings,
                    heartbeat: Int,
                    plannerHeartbeat: Int,
                    blockPeriod: Long,
                    canPublishBlocks: Boolean,
                    biasForBlockPeriod: Long,
                    apiSettings: ApiSettings,
                    ntp: NetworkTimeProviderSettings,
                    influx: InfluxSettings,
                    testingSettings: TestingSettings,
                    mempoolSetting: MempoolSetting,
                    ethereumSettings: EthereumSettings
                   )

case class Node(host: String, port: Int)

case class ApiSettings(httpHost: String, httpPort: Int, timeout: Int)

case class InfluxSettings(host: String, port: Int, login: String, password: String)

case class NetworkTimeProviderSettings(server: String, updateEvery: Int, timeout: Int)

case class MempoolSetting(transactionsValidTime: Long, mempoolCleaningTime: Long)

case class TestingSettings(messagesTime: Boolean, iteratorsSyncTime: Int)

case class NetworkSettings(maxBlockQtyInBlocksMessage: Int)

case class EthereumSettings(userAccount: String, userPassword: String, receiverAccount: String,
                            peerRPCAddress: String, gasPrice: Long)