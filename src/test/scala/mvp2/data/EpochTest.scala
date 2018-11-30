package mvp2.data

import akka.util.ByteString
import mvp2.actors.Planner.Epoch
import org.scalatest.{Matchers, PropSpecLike}

class EpochTest extends PropSpecLike with Matchers {

  val publicKeys1: List[ByteString] = List(
    ByteString("11qwertynddsvm"),
    ByteString("22qwertynddsvmwerf"),
    ByteString("33qwertynwerqewrddsvm"),
    ByteString("44qwertynddsvmqwerqw"),
    ByteString("55qwertyndd1241svm"),
  )

  val publicKeys2: List[ByteString] = List(ByteString("11qwertynddsvm"))

  property("Epoch size must me 50 after apply method:") {
    val epoch: Epoch = Epoch(publicKeys1, 10)
    epoch.schedule.size shouldEqual 50
  }

  property("Epoch size must me 10 after apply method with 1 l element in keys set:") {
    val epoch: Epoch = Epoch(publicKeys2, 10)
    epoch.schedule.size shouldEqual 10
  }

}