package migrate

object NameConflict {
  case object File
  def a: NameConflict.File.type = File
  def b: java.io.File = null.asInstanceOf[java.io.File]
}