
package migrate

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: Path.this.B = new B
    implicit val y: Path.this.x.C = new x.C
    def gimme(yy: x.C): Nothing = identity[Nothing](???); gimme(y)
  }
  implicit val b: migrate.ExplicitResultTypesPathDependent.Path#B = new Path().x
  implicit val c: scala.collection.immutable.List[migrate.ExplicitResultTypesPathDependent.Path#B] = List.apply[migrate.ExplicitResultTypesPathDependent.Path#B](new Path().x)
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T]: migrate.ExplicitResultTypesPathDependent.Foo[T]#Self = null.asInstanceOf[Foo[T]].bar
}
