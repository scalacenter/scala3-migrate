import scala.language.reflectiveCalls

object ReflectiveCall {
  
  def test(): Unit = {
    val foo = new {
      def bar: Unit = ???
    }
    foo.bar
  }
}