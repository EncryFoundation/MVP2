package mvp2.actors

import akka.actor.ActorSelection
import akka.persistence.RecoveryCompleted
import mvp2.data.Block
import mvp2.utils.DbService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class PgReader(dbService: DbService, batchSize: Int) extends CommonActor {

  val blockchainer: ActorSelection = context.actorSelection("/user/starter/blockchainer")

  dbService
    .selectMaxHeightOpt()
    .recoverWith {
      case NonFatal(th) =>
        blockchainer ! RecoveryCompleted
        logger.warn("Failed to connect to postgres")
        context.stop(self)
        Future.failed(th)
    }
    .foreach(self ! _.getOrElse(0))

  override def specialBehavior: Receive = {
    case height: Int => (0 to height).sliding(batchSize, batchSize).foldLeft(Future.successful(List[Block]())) {
      case (prevBlocks, slide) =>
        val from: Int = slide.head
        val to: Int = slide.last
        logger.info(s"Trying to download blocks $from-$to from postgres")
        prevBlocks.flatMap { retrieved =>
          if (retrieved.nonEmpty)logger.info(s"Downloaded blocks ${retrieved.head.height}-${retrieved.last.height} from postgres")
          retrieved.foreach(blockchainer ! _)
          if (to == height) blockchainer ! RecoveryCompleted
          dbService.selectBlocksInHeightRange(from, to)
        }.recoverWith {
          case NonFatal(th) =>
            logger.warn("Failed to download blocks", th)
            blockchainer ! RecoveryCompleted
            Future.failed(th)
        }
    }
  }
}
