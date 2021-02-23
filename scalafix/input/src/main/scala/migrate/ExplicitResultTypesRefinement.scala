/*
rule = [MigrationRule]
*/
package migrate

import java.io.Serializable
import scala.language.reflectiveCalls

object ExplicitResultTypesRefinement {
  val field = new Serializable {
    val results: List[Int] = List(1)
  }
  val conflict = new Serializable {
    val results: List[Int] = List(1)
  }
  class conflict
  class conflict1
  def method(param: Int) = new Serializable {
    val results: List[Int] = List(param)
  }
  def method(param: String) = new Serializable {
    val results: List[String] = List(param)
  }
  def curried(param: Int)(param2: Int, param3: String) = new Serializable {
    val results: List[Int] = List(param2, param3.length(), param)
  }
  def tparam[T <: CharSequence](e: T) = new Serializable {
    val results: List[Int] = List(e.length())
  }
  val access = new Serializable {
    private val results: List[Int] = List.empty
    protected val results2: List[Int] = List.empty
  }
  val product = new Product {
    override def productArity: Int = ???
    override def productElement(n: Int): Any = ???
    override def canEqual(that: Any): Boolean = ???
  }
  val productWithSerializable = new Product with Serializable {
    override def productArity: Int = ???
    override def productElement(n: Int): Any = ???
    override def canEqual(that: Any): Boolean = ???
  }
  val test = new conflict with Product with Serializable {
    override def productArity: Int = ???
    override def productElement(n: Int): Any = ???
    override def canEqual(that: Any): Boolean = ???
  }
  trait Chars { def chars: CharSequence }
  val chars = new Chars {
    val chars = 42.toString()
  }
  def app(): Unit = {
    println(field.results)
    println(method(42).results)
  }
}
