package migrate

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: B = new B
    implicit val y: x.C = new x.C
    def gimme(yy: x.C): Nothing = identity[Nothing](???); gimme(y)
  }
  implicit val b: Path#B = new Path().x
  implicit val c: List[Path#B] = List.apply[Path#B](new Path().x)
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T]: Foo[T]#Self = null.asInstanceOf[Foo[T]].bar
}