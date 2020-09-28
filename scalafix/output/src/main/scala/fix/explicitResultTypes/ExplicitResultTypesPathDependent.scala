
package fix.explicitResultTypes

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: Path.this.B = new B
    implicit val y: Path.this.x.C = new x.C
    def gimme(yy: x.C): Nothing = identity(???); gimme(y)
  }
  implicit val b: fix.explicitResultTypes.ExplicitResultTypesPathDependent.Path#B = new Path().x
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T]: fix.explicitResultTypes.ExplicitResultTypesPathDependent.Foo[T]#Self = null.asInstanceOf[Foo[T]].bar
}
