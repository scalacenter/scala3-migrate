/*
rule = [InferTypes, ExplicitImplicits]
*/
package migrate

object PartialFunction {
  def empty[A, B] = scala.PartialFunction.empty[A, B]
}
