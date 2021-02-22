/*
rule = [MigrationRule]
*/
package migrate

object NameConflict {
  case object File
  def a = File
  def b = null.asInstanceOf[java.io.File]
}
