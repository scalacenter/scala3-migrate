import scala.language.reflectiveCalls

object ReflectiveCall {
  
  def test(): Unit = {
    val foo: AnyRef{def bar: Unit} = new {
      def bar: Unit = ???
    }
    foo.bar
  }
}