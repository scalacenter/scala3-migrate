package migrate

import java.lang.{Boolean => JBoolean}

import scala.collection.immutable.{List => LList}

object Rename {
  type List = Int
  def foo(a: Int): scala.collection.immutable.List[Int] = List.apply[Int](a)
  def foo: Boolean = identity[Boolean](JBoolean.TRUE)
}
