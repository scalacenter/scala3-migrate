/*
rules = MigrationRule
*/
package migrate

object NameConflict {
  def a = null.asInstanceOf[scala.reflect.io.File]
  def b = null.asInstanceOf[java.io.File]
}
