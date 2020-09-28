package fix.explicitResultTypes

object NameConflict {
  def a: scala.reflect.io.File = null.asInstanceOf[scala.reflect.io.File]
  def b: java.io.File = null.asInstanceOf[java.io.File]
}
