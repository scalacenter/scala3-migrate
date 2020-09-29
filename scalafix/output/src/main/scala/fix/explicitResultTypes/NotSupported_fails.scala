package fix.explicitResultTypes

import java.nio.file.Paths

object NotSupported_fails {
  def path = Paths.get("")
  val java: _root_.java.nio.file.Path = path


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

  implicit val implicit_x: Int = 42
  implicit val crazy1: Int = implicitly[Int](implicit_x)

  class H[M[_]]

  val universalType1 = ??? : H[({ type L[U] = List[U] })#L]

  // FIXME: https://github.com/twitter/rsc/issues/144
//   val repeatedType = ??? : ((Any*) => Any)
}
