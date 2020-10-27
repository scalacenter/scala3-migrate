package migrate

case class PathName private (value: String) {
  require(PathName.hasErrors(value), s"$value is not a PathName!")

  override def toString: String = value
}

object PathName {
  def hasErrors(value: String): Boolean = !value.contains("/")
}
