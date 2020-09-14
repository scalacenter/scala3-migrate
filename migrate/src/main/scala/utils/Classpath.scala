package utils

case class Classpath(paths: AbsolutePath*) {
  val value: String = paths.mkString(":")
}