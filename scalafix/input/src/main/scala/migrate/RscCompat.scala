/*
rule = [InferTypes, ExplicitImplicits]
*/
package migrate

import java.util.Base64
import scala.language.existentials

object RscCompat {
  class Basic {
    def x1 = 42
    val x2 = ""
    final val x3 = ""
    var x4 = ""
  }

  class Patterns {
    val List() = List()
    val List(x2) = List(2)
    val List(x3, y3) = List(3, 3)
    val x4, y4 = 4
    val x9 :: y9 = List(9, 9, 9)
    var List() = List()
    var List(x6) = List(6)
    var List(x7, y7) = List(7, 7)
    var x8, y8 = 8
    var x10 :: y10 = List(10, 10, 10)
  }

  class Visibility {
    private def x1 = ""
    private[this] def x2 = ""
    private[migrate] def x3 = ""
    protected def x4 = ""
    protected[this] def x5 = ""
    protected[migrate] def x6 = ""
  }

  private class Private { def x1 = "" }
  private[this] class PrivateThis { def x1 = "" }
  private[migrate] class PrivateRsc { def x1 = "" }
  protected class Protected { def x1 = "" }
  protected[this] class ProtectedThis { def x1 = "" }
  protected[migrate] class ProtectedRsc { def x1 = "" }

  private trait PrivateTrait { def x1 = "" }
  trait PublicTrait {
    private def x1 = ""
    private[this] def x2 = ""
    private[migrate] def x3 = ""
    protected def x4 = ""
    protected[this] def x5 = ""
    protected[migrate] def x6 = ""
  }

  object Config {
    val x = 1
    val y = 2
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
    import TypesHelpers._
    override val x: X = new X

    val typeRef1 = ??? : C
    val typeRef2 = ??? : p.C
    val typeRef3 = ??? : E#C
    val typeRef4 = ??? : List[Int]
    val typeRef5 = ??? : X
    val typeRef6 = ??? : T
    val typeRef7 = ??? : (() => T)
    val typeRef8 = ??? : (T => T)
    val typeRef9 = ??? : ((T, T) => T)
    val typeRef10 = ??? : (T, T)

    val singleType1 = ??? : c.type
    val singleType2 = ??? : p.c.type

    val Either = ??? : scala.util.Either.type

     val thisType1 = ??? : this.type
     val thisType2 = ??? : Types.this.type

    val compoundType1 = ??? : { def k: Int }
    val compoundType2 = ??? : A with B
    val compoundType3 = ??? : A with B { def k: Int }
    val compoundType4 = new { def k: Int = ??? }
    val compoundType5 = new A with B
    val compoundType6 = new A with B { def k: Int = ??? }
//    val compoundType7 = ??? : A with (List[T] forSome { type T }) with B

     val annType1 = ??? : C @ann

//    val existentialType1 = ??? : T forSome { type T }
//    val existentialType2 = ??? : List[_]

    val byNameType = ??? : ((=> Any) => Any)
  }

  implicit val implicit_x: Int = 42
  implicit val implicit_y: String = "42"
  trait In
  trait Out1
  trait Out2

  class Bugs {
    val Either = scala.util.Either

    implicit def order[T] = new Ordering[List[T]] {
      def compare(a: List[T], b: List[T]): Int = ???
    }

    val gauges = {
      val local1 = 42
      val local2 = 43
      List(local1, local2)
    }

     val clazz = Class.forName("foo.Bar")

    val compound = ??? : { def m(x: Int): Int }

    val loaded = List[Class[_]]()

    val ti: Types[Int] = ???
    val innerClass1 = new Types[String]().x
    val innerClass2 = ??? : Types[Int]#X
    val innerClass3 = ti.x
    val innerClass4 = Base64.getMimeDecoder

    val sane1 = implicitly[String]

    val t = ??? : foo.T

    val (a: String, b: String, c) = ("foo", "bar", "baz")
  }
}

private class RscCompat_Test {
  def x1 = ""
}

package object foo {
  class T
}
