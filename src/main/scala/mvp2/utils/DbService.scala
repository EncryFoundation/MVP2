package mvp2.utils

import cats.effect.IO
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.hikari.HikariTransactor
import doobie.postgres.implicits._
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.implicits._
import doobie.util.update.Update
import mvp2.actors.Accountant.Account
import mvp2.data.{Block, KeyBlock, MicroBlock, Transaction}
import mvp2.utils.DbService.BlockDbVersion
import mvp2.utils.EncodingUtils._
import scala.concurrent.Future

class DbService(settings: PostgresSettings) {

  import Queries._

  def writeBlock(block: Block): Future[Int] = runAsync(writeBlockQuery(block))
  def writeAccount(account: Account): Future[Int] = runAsync(writeAccountQuery(account))

  private val dataSource = new HikariDataSource
  dataSource.setJdbcUrl(s"${settings.host}?loggerLevel=OFF")
  dataSource.setUsername(settings.user)
  dataSource.setPassword(settings.pass)
  dataSource.setMaximumPoolSize(1)

  private val pgTransactor: HikariTransactor[IO] = HikariTransactor[IO](dataSource)

  def shutdown(): Future[Unit] = pgTransactor.shutdown.unsafeToFuture

  private def runAsync[A](io: ConnectionIO[A]): Future[A] =
    (for {
      res <- io.transact(pgTransactor)
    } yield res)
      .unsafeToFuture()
}

private object DbService {
  case class BlockDbVersion(isMicroBlock: Boolean,
                            height: Long,
                            timestamp: Long,
                            prevKeyBlockHash: Array[Byte],
                            currentBlockHash: String,
                            data: Array[Byte],
                            prevMicroBlock: Option[Array[Byte]])

  object BlockDbVersion {
    def apply(block: Block): BlockDbVersion = block match {
      case micro: MicroBlock =>
        BlockDbVersion(
          true,
          micro.height,
          micro.timestamp,
          micro.previousKeyBlockHash.toArray,
          encode2Base64(micro.currentBlockHash),
          micro.data.toArray,
          Some(micro.previousMicroBlock.toArray)
        )
      case key: KeyBlock =>
        BlockDbVersion(
          false,
          key.height,
          key.timestamp,
          key.previousKeyBlockHash.toArray,
          encode2Base64(key.currentBlockHash),
          key.data.toArray,
          None
        )
    }
  }
}

private object Queries {

  def writeAccountQuery(account: Account): ConnectionIO[Int] = for {
    accW <- writeAccountInfoQuery(account)
    dataW <- writeAccountDataQuery(account)
  } yield accW + dataW

  def writeAccountInfoQuery(account: Account): ConnectionIO[Int] = {
    val query: String =
      """
        |INSERT INTO public.accounts (public_key, nonce) VALUES(?, ?) ON CONFLICT (public_key) DO NOTHING
      """.stripMargin
    Update[(String, Long)](query).run((encode2Base64(account.publicKey), account.nonce))
  }

  def writeAccountDataQuery(account: Account): ConnectionIO[Int] = {
    val query: String =
      """
        |INSERT INTO public.accounts_data (public_key, number_in_account, data) VALUES(?, ?, ?) ON CONFLICT (public_key, number_in_account) DO NOTHING
      """.stripMargin
    Update[(String, Int, Array[Byte])](query)
      .updateMany(account.data.zipWithIndex.map(entry => (encode2Base64(account.publicKey), entry._2, entry._1.toArray)))
  }

  def writeBlockQuery(block: Block): ConnectionIO[Int] = for {
    blockW <- writeBlockInfoQuery(block)
    txsW <- block match {
      case key: KeyBlock => writeTransactionsQuery(key.transactions, block)
      case micro: MicroBlock => writeTransactionsQuery(micro.transactions, block)
    }
  } yield blockW + txsW


  def writeBlockInfoQuery(block: Block): ConnectionIO[Int] = {
    val blockDto: BlockDbVersion = BlockDbVersion(block)
    val query: String =
      """
        |INSERT INTO public.blocks (is_microblock, height, timestamp, previous_key_block_hash, current_block_hash,
        |  data, prev_microblock) VALUES(?, ?, ?, ?, ?, ?, ?)
      """.stripMargin
    Update[BlockDbVersion](query).run(blockDto)
  }

  def writeTransactionsQuery(transactions: List[Transaction], block: Block): ConnectionIO[Int] = {
    val query: String =
      """
        |INSERT INTO transactions(public_key, nonce, signature, data, block_hash) VALUES(?, ?, ?, ?, ?)
      """.stripMargin
    Update[(String, Long, Array[Byte], Array[Byte], String)](query)
      .updateMany(transactions.map { tx =>
        (encode2Base64(tx.publicKey), tx.nonce, tx.signature.toArray, tx.data.toArray, encode2Base64(block.currentBlockHash))
      })
  }

}