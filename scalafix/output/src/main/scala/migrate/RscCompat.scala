package migrate

import java.util.Base64
import scala.language.existentials

object RscCompat {
  class Basic {
    def x1: Int = 42
    val x2: String = ""
    final val x3 = ""
    var x4: String = ""
  }

  class Patterns {
    val List() = List.apply[Nothing]()
    val List(x2) = List.apply[Int](2)
    val List(x3, y3) = List.apply[Int](3, 3)
    val x4, y4 = 4
    val x9 :: y9 = List.apply[Int](9, 9, 9)
    var List() = List.apply[Nothing]()
    var List(x6) = List.apply[Int](6)
    var List(x7, y7) = List.apply[Int](7, 7)
    var x8, y8 = 8
    var x10 :: y10 = List.apply[Int](10, 10, 10)
  }

  class Visibility {
    private def x1: String = ""
    private[this] def x2: String = ""
    private[migrate] def x3: String = ""
    protected def x4: String = ""
    protected[this] def x5: String = ""
    protected[migrate] def x6: String = ""
  }

  private class Private { def x1: String = "" }
  private[this] class PrivateThis { def x1: String = "" }
  private[migrate] class PrivateRsc { def x1: String = "" }
  protected class Protected { def x1: String = "" }
  protected[this] class ProtectedThis { def x1: String = "" }
  protected[migrate] class ProtectedRsc { def x1: String = "" }

  private trait PrivateTrait { def x1: String = "" }
  trait PublicTrait {
    private def x1: String = ""
    private[this] def x2: String = ""
    private[migrate] def x3: String = ""
    protected def x4: String = ""
    protected[this] def x5: String = ""
    protected[migrate] def x6: String = ""
  }

  object Config {
    val x: Int = 1
    val y: Int = 2
  }

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

  trait TypesBase {
    class X
    val x: X = ???
  }

  class Types[T] extends TypesBase {
    import TypesHelpers._
    override val x: X = new X

    val typeRef1: C = ??? : C
    val typeRef2: p.C = ??? : p.C
    val typeRef3: E#C = ??? : E#C
    val typeRef4: List[Int] = ??? : List[Int]
    val typeRef5: X = ??? : X
    val typeRef6: T = ??? : T
    val typeRef7: () => T = ??? : (() => T)
    val typeRef8: T => T = ??? : (T => T)
    val typeRef9: (T, T) => T = ??? : ((T, T) => T)
    val typeRef10: (T, T) = ??? : (T, T)

    val singleType1: c.type = ??? : c.type
    val singleType2: p.c.type = ??? : p.c.type

    val Either: util.Either.type = ??? : scala.util.Either.type

     val thisType1: Types.this.type = ??? : this.type
     val thisType2: Types.this.type = ??? : Types.this.type

    val compoundType1: AnyRef{def k: Int} = ??? : { def k: Int }
    val compoundType2: A with B = ??? : A with B
    val compoundType3: A with B{def k: Int} = ??? : A with B { def k: Int }
    val compoundType4: AnyRef{def k: Int} = new { def k: Int = ??? }
    val compoundType5: A with B = new A with B
    val compoundType6: A with B{def k: Int} = new A with B { def k: Int = ??? }
//    val compoundType7 = ??? : A with (List[T] forSome { type T }) with B

     val annType1: migrate.RscCompat.TypesHelpers.C @migrate.RscCompat.TypesHelpers.ann = ??? : C @ann

//    val existentialType1 = ??? : T forSome { type T }
//    val existentialType2 = ??? : List[_]

    val byNameType: (=> Any) => Any = ??? : ((=> Any) => Any)
  }

  implicit val implicit_x: Int = 42
  implicit val implicit_y: String = "42"
  trait In
  trait Out1
  trait Out2

  class Bugs {
    val Either: util.Either.type = scala.util.Either

    implicit def order[T]: Ordering[List[T]] = new Ordering[List[T]] {
      def compare(a: List[T], b: List[T]): Int = ???
    }

    val gauges: List[Int] = {
      val local1: Int = 42
      val local2: Int = 43
      List.apply[Int](local1, local2)
    }

     val clazz: Class[_ <: Object] = Class.forName("foo.Bar")

    val compound: AnyRef{def m(x: Int): Int} = ??? : { def m(x: Int): Int }

    val loaded: List[Class[_]] = List[Class[_]]()

    val ti: Types[Int] = ???
    val innerClass1: Types[String]#X = new Types[String]().x
    val innerClass2: Types[Int]#X = ??? : Types[Int]#X
    val innerClass3: ti.X = ti.x
    val innerClass4: Base64.Decoder = Base64.getMimeDecoder

    val sane1: String = implicitly[String](migrate.RscCompat.implicit_y)

    val t: foo.T = ??? : foo.T

    val (a: String, b: String, c) = ("foo", "bar", "baz")
  }
}

private class RscCompat_Test {
  def x1: String = ""
}

package object foo {
  class T
}