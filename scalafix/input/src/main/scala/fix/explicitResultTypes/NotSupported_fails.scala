/*
rule = Infertypes
*/
package fix.explicitResultTypes

import java.nio.file.Paths

object NotSupported_fails {
  def path = Paths.get("")
  val java = path

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

  trait TypesBase {
    class X
    val x: X = ???
  }

  class Types[T] extends TypesBase {
    override val x: X = new X

    // FIXME: https://github.com/twitter/rsc/issues/143
    val superType1 = ??? : super.x.type
    val superType2 = ??? : super[TypesBase].x.type
    val superType3 = ??? : Types.super[TypesBase].x.type
  }

  class H[M[_]]

  val universalType1 = ??? : H[({ type L[U] = List[U] })#L]
}
