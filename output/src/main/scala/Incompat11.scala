//import scala.concurrent.Future
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{Await, Future}
//import scala.concurrent.duration._
//object Incompat11 {
//  class Foo() {
//    val status: Int = 0
//  }
//  implicit class RichFuture[T](future: Future[T]) {
//    def await(implicit duration: Duration = 10.seconds): T = Await.result(future, duration)
//  }
//  def test = Future(new Foo())(global).await.status
//}