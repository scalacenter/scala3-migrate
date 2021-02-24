package migrate

import java.lang.{Boolean => JBoolean}

import scala.collection.immutable.{List => LList}

object Rename {
//  type List = Int
  def foo(a: Int): List[Int] = List.apply[Int](a)
  def foo: java.lang.Boolean = identity[java.lang.Boolean](JBoolean.TRUE)
}