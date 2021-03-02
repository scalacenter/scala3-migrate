package migrate

import scala.collection.{mutable => mut}

object ExplicitResultTypesDiverse {
  
  val map: collection.mutable.Map[Int,Int] = mut.Map.empty[Int, Int]

  object Ignored {
    import java.{util => ju}
    val hasImport: java.util.List[Int] = ju.Collections.emptyList[Int]()
  }
  val missingImport: java.util.List[Int] = java.util.Collections.emptyList[Int]()

  def o3: testpkg.O3 = new testpkg.O3()

  def overload(a: Int): Int = a
  def overload(a: String): String = a

  abstract class ParserInput {
    def apply(index: Int): Char
  }
  case class IndexedParserInput(data: String) extends ParserInput {
    override def apply(index: Int): Char = data.charAt(index)
  }
  case class Foo(a: Int) {
    def apply(x: Int): Int = x
  }
  abstract class Opt[T] {
    def get(e: T): T
  }
  class IntOpt extends Opt[Int] {
    def get(e: Int): Int = e
  }

  val o4: List[testpkg.O4] = null.asInstanceOf[List[testpkg.O4]]

}