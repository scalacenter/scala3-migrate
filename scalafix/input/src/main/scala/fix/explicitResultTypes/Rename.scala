/*
rules = ExplicitResultTypes
*/
package fix.explicitResultTypes

import java.lang.{Boolean => JBoolean}

import scala.collection.immutable.{List => LList}

object Rename {
  type List = Int
  def foo(a: Int) = List(a)
  def foo = identity(JBoolean.TRUE)
}
