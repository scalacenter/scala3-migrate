package migrate

import java.io.Serializable
import scala.language.reflectiveCalls

object ExplicitResultTypesRefinement {
  val field: java.io.Serializable{val results: List[Int]} = new Serializable {
    val results: List[Int] = List.apply[Int](1)
  }
  val conflict: java.io.Serializable{val results: List[Int]} = new Serializable {
    val results: List[Int] = List.apply[Int](1)
  }
  class conflict
  class conflict1
  def method(param: Int): java.io.Serializable{val results: List[Int]} = new Serializable {
    val results: List[Int] = List.apply[Int](param)
  }
  def method(param: String): java.io.Serializable{val results: List[String]} = new Serializable {
    val results: List[String] = List.apply[String](param)
  }
  def curried(param: Int)(param2: Int, param3: String): java.io.Serializable{val results: List[Int]} = new Serializable {
    val results: List[Int] = List.apply[Int](param2, param3.length(), param)
  }
  def tparam[T <: CharSequence](e: T): java.io.Serializable{val results: List[Int]} = new Serializable {
    val results: List[Int] = List.apply[Int](e.length())
  }
  val access: java.io.Serializable = new Serializable {
    private val results: List[Int] = List.empty[scala.Nothing]
    protected val results2: List[Int] = List.empty[scala.Nothing]
  }
  val product: Product = new Product {
    override def productArity: Int = ???
    override def productElement(n: Int): Any = ???
    override def canEqual(that: Any): Boolean = ???
  }
  val productWithSerializable: Product with java.io.Serializable = new Product with Serializable {
    override def productArity: Int = ???
    override def productElement(n: Int): Any = ???
    override def canEqual(that: Any): Boolean = ???
  }
  val test: migrate.ExplicitResultTypesRefinement.conflict with Product with java.io.Serializable = new conflict with Product with Serializable {
    override def productArity: Int = ???
    override def productElement(n: Int): Any = ???
    override def canEqual(that: Any): Boolean = ???
  }
  trait Chars { def chars: CharSequence }
  val chars: migrate.ExplicitResultTypesRefinement.Chars{val chars: String} = new Chars {
    val chars: String = 42.toString()
  }
  def app(): Unit = {
    println(field.results)
    println(method(42).results)
  }
}
