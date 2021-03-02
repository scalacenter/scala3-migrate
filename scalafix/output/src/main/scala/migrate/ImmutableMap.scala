package migrate

import scala.collection.concurrent.Map

object ImmutableMap {
  def foo: collection.immutable.Map[Int,Int] = null.asInstanceOf[scala.collection.immutable.Map[Int, Int]]
  def bar: Map[Int,Int] = null.asInstanceOf[Map[Int, Int]]
}