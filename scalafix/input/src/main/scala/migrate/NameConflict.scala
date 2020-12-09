/*
rule = [InferTypes, ExplicitImplicits]
*/
package migrate

object NameConflict {
  case object File
  def a = File
  def b = null.asInstanceOf[java.io.File]
}
