package incompat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Incompat11 {
  class Foo() {
    val status: Int = 0
  }
  implicit class RichFuture[T](future: Future[T]) {
    def await(implicit duration: Duration = DurationInt(10).seconds): T = Await.result[T](future, duration)
  }
  def test: Int = Future.apply[incompat.Incompat11.Foo](new Foo())(scala.concurrent.ExecutionContext.Implicits.global).await.status
}