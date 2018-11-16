package mvp2.data

import mvp2.utils.Settings

case class Mempool(settings: Settings, var mempool: List[Transaction] = List()) {

  def isContains(transaction: Transaction): Boolean = mempool.contains(transaction)

  def checkMempoolForInvalidTxs: Unit =
    mempool = mempool.filter(transaction =>
      transaction.timestamp < System.currentTimeMillis() - settings.mempoolSetting.transactionsValidTime
    )

  def removeUsedTxs(usedTxs: List[Transaction]): Unit = mempool = mempool.diff(usedTxs)

  def updateMempool(transaction: Transaction): Boolean =
    if (!isContains(transaction)) {
      mempool = transaction :: mempool
      true
    } else false

  def cleanMempool: Unit = mempool = List()
}