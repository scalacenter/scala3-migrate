package migrate

import scala.language.implicitConversions

object ExplicitResultTypesBase {
  def none[T]: Option[T] =  None.asInstanceOf[Option[T]]
  val a: Int = 1 + 2
  def b(): String = "a" + "b"
  var c: Boolean = 1 == 1
  protected val d: Float = 1.0f
  protected def e(a: Int, b: Double): Double = a + b
  protected var f: Int => Int = (x: Int) => x + 1
  val f0: () => Int = () => 42
  private val g: Int = 1
  private def h(a: Int): String = ""
  private var i: Int = 22
  private implicit var j: Int = 1
  val k: (Int, String) = (1, "msg")
  implicit val L: List[Int] = List.apply[Int](1)
  implicit val M: Map[Int,String] = Map.apply[Int, String](1 -> "STRING")
  implicit def D: Int = 2
  implicit def tparam[T](e: T): T = e
  implicit def tparam2[T](e: T): List[T] = List.apply[T](e)
  implicit def tparam3[T](e: T): Map[T,T] = Map.apply[T, T](e -> e)
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
//    implicit val x = implicitly[String] // adding type here will fail compilation
  }
  def comment(x: Int): Int =
  // comment
    x + 2
  object ExtraSpace {
    def * : Int = "abc".length
    def foo_ : Int = "abc".length
    def `x`: Int = "abc".length
    def `x `: Int = "abc".length
  }
  locally[Unit] {
    implicit val Implicit: scala.concurrent.Future[Int] = scala.concurrent.Future.successful[Int](2)
    val Var: scala.concurrent.Future[Int] = scala.concurrent.Future.successful[Int](2)
    val Val: scala.concurrent.Future[Int] = scala.concurrent.Future.successful[Int](2)
    def Def: scala.concurrent.Future[Int] = scala.concurrent.Future.successful[Int](2)
  }
  object unicode {
    object `->` {
      def unapply[S](in: (S, S)): Option[(S, S)] = Some.apply[(S, S)](in)
    }
    val `→`: migrate.ExplicitResultTypesBase.unicode.->.type = `->`
  }
  def tuple: ((Int, String)) => String = null.asInstanceOf[((Int, String)) => String]
}

