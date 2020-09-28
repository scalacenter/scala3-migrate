package fix.explicitResultTypes

import java.lang.{Boolean => JBoolean}

import scala.collection.immutable.{List => LList}

object Rename {
  type List = Int
  def foo(a: Int): LList[Int] = List(a)
  def foo: JBoolean = identity(JBoolean.TRUE)
}
