/*
rule = [MigrationRule]
*/
package migrate

object ScalaPackage {
  class Try
  def a = null.asInstanceOf[scala.util.Try[Int]]
}
