package migrate

object NotSupported_fails {

  val param = new { lazy val default: Boolean = true }
  val more1: AnyRef{def x: Int; def x_=(x$1: Int): Unit} = new { var x: Int = 42 }
  val more2 = new { def foo(implicit x: Int, y: Int): Int = 42 }
  val more3 = new { implicit def bar: Int = 42 }

  object TypesHelpers {
    class C
    class E {
      class C
    }
    class P {
      class C
      val c: C = ???
    }
    val p: P = new P
    val c: p.C = p.c
    trait A
    trait B
    class ann extends scala.annotation.StaticAnnotation
    class H[M[_]]
  }

  class H[M[_]]

  val universalType1 = ??? : H[({ type L[U] = List[U] })#L]
}