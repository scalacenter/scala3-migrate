package fix.explicitResultTypes

import scala.collection.concurrent.Map

object ImmutableMap {
  def foo: scala.collection.immutable.Map[Int,Int] = null.asInstanceOf[scala.collection.immutable.Map[Int, Int]]
  def bar: scala.collection.concurrent.Map[Int,Int] = null.asInstanceOf[Map[Int, Int]]
}
