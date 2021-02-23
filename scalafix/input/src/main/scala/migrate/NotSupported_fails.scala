/*
rule = [MigrationRule]
*/
package migrate

object NotSupported_fails {

  val param = new { lazy val default = true }
  val more1 = new { var x = 42 }
  val more2 = new { def foo(implicit x: Int, y: Int) = 42 }
  val more3 = new { implicit def bar = 42 }

  object TypesHelpers {
    class C
    class E {
      class C
    }
    class P {
      class C
      val c: C = ???
    }
    val p = new P
    val c = p.c
    trait A
    trait B
    class ann extends scala.annotation.StaticAnnotation
    class H[M[_]]
  }

  class H[M[_]]

  val universalType1 = ??? : H[({ type L[U] = List[U] })#L]
}
