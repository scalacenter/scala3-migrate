/*
rule = [MigrationRule]
*/
package migrate

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x = new B
    implicit val y = new x.C
    def gimme(yy: x.C) = identity(???); gimme(y)
  }
  implicit val b = new Path().x
  implicit val c = List(new Path().x)
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T] = null.asInstanceOf[Foo[T]].bar
}