package mvp2.actors

import mvp2.actors.Accountant.Account
import mvp2.data.Block
import mvp2.utils.DbService

class PgWriter(dbService: DbService) extends CommonActor {

  override def specialBehavior: Receive = {
    case block: Block => dbService.writeBlock(block)
    case account: Account => dbService.writeAccount(account)
  }

  override def postStop(): Unit = dbService.shutdown()
}
