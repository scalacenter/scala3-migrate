/*
rule = [InferTypes, ExplicitImplicits]
*/
package migrate

import scala.collection.immutable.Seq

object ImmutableSeq {
  def seq(): collection.Seq[Int] = Seq.empty[Int]
  def scalaSeq(): scala.Seq[Int] = Seq.empty[Int]
  def foo = seq()
  def scalaFoo = scalaSeq()

  def foo(a: Int*) = identity(a.toList)
}
