package fix.explicitResultTypes

trait WidenSingleType {
  object param extends Ordering[Int] {
    def compare(x: Int, y: Int): Int = ???
    def foo: Int = 5
  }
  object strings {
    val message: String = "Hello!"
  }
  trait Meaningful
  object Meaningful extends Meaningful {
    val message: String = "Hello!"
  }
}
abstract class WidenSingleTypeUsage {
  def widen: WidenSingleType
  def widenString = widen.strings // left un-annotated
  def meaningful = widen.Meaningful
  def message: String = identity(meaningful.message)
}
object WidenSingleType {
  def list(a: WidenSingleType, b: WidenSingleType) =
    Seq(a.param, b.param)
  def strings(a: WidenSingleType): a.strings.type = identity(a.strings)
  def k: Int = list(???, ???).head.foo
}
