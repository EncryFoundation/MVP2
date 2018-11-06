package mvp2.utils

import akka.util.ByteString
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
import mvp2.utils.DbService.{BlockDbVersion, TransactionDbVersion}
import mvp2.utils.EncodingUtils._
import scala.concurrent.Future

class DbService(settings: PostgresSettings) {

  import Queries._

  def writeBlock(block: Block): Future[Int] = runAsync(writeBlockQuery(block))
  def writeAccount(account: Account): Future[Int] = runAsync(writeAccountQuery(account))
  def selectMaxHeightOpt(): Future[Option[Int]] = runAsync(selectMaxHeightOptQuery)
  def selectBlocksInHeightRange(from: Int, to: Int): Future[List[Block]] = runAsync(selectBlocksByRangeQuery(from, to))

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

  case class TransactionDbVersion(publicKey: String, nonce: Long, signature: Array[Byte], data: Array[Byte], blockHash: String) {
    def toTransaction: Transaction = Transaction(decodeFromBase64(publicKey), nonce, ByteString(signature), ByteString(data))
  }
  object TransactionDbVersion {
    def apply(txs: List[Transaction], block: Block): List[TransactionDbVersion] = txs.map { tx =>
      TransactionDbVersion(
        encode2Base64(tx.publicKey),
        tx.nonce,
        tx.signature.toArray,
        tx.data.toArray,
        encode2Base64(block.currentBlockHash)
      )
    }
  }
}

private object Queries {

  def selectBlocksByRangeQuery(from: Int, to: Int): ConnectionIO[List[Block]] = for {
    infos <- selectBlocksInfosByRange(from, to)
    txs   <- selectTransactionsForBlocks(from, to)
  } yield {
    infos.map { dbBlock =>
      val relatedTxs: List[Transaction] = txs.filter(_.blockHash == dbBlock.currentBlockHash).map(_.toTransaction)
      if (dbBlock.isMicroBlock)
        MicroBlock(
          dbBlock.height,
          dbBlock.timestamp,
          ByteString(dbBlock.prevKeyBlockHash),
          ByteString(dbBlock.prevMicroBlock.getOrElse(Array.empty[Byte])),
          ByteString(dbBlock.currentBlockHash),
          relatedTxs,
          ByteString(dbBlock.data)
        )
      else
        KeyBlock(
          dbBlock.height,
          dbBlock.timestamp,
          ByteString(dbBlock.prevKeyBlockHash),
          ByteString(dbBlock.currentBlockHash),
          relatedTxs,
          ByteString(dbBlock.data)
        )
    }
  }

  def selectBlocksInfosByRange(from: Int, to: Int): ConnectionIO[List[BlockDbVersion]] =
    sql"SELECT * FROM public.blocks WHERE height >= $from AND height <= $to ORDER BY height ASC".query[BlockDbVersion].to[List]

  def selectTransactionsForBlocks(from: Int, to: Int): ConnectionIO[List[TransactionDbVersion]] =
    sql"SELECT * FROM public.transactions WHERE block_hash IN (SELECT current_block_hash FROM public.blocks WHERE height >= $from AND height <= $to)"
      .query[TransactionDbVersion].to[List]

  def selectMaxHeightOptQuery: ConnectionIO[Option[Int]] = sql"SELECT MAX(height) FROM public.blocks".query[Option[Int]].unique

  def writeAccountQuery(account: Account): ConnectionIO[Int] = for {
    accW  <- writeAccountInfoQuery(account)
    dataW <- writeAccountDataQuery(account)
  } yield accW + dataW

  def writeAccountInfoQuery(account: Account): ConnectionIO[Int] = {
    val query: String =
      """
        |INSERT INTO public.accounts (public_key, nonce) VALUES(?, ?) ON CONFLICT (public_key) DO UPDATE SET nonce = EXCLUDED.nonce
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
    txsW   <- block match {
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
        |INSERT INTO public.transactions(public_key, nonce, signature, data, block_hash) VALUES(?, ?, ?, ?, ?)
      """.stripMargin
    Update[TransactionDbVersion](query).updateMany(TransactionDbVersion(transactions, block))
  }

}