package domain

case class Classpath(paths: AbsolutePath*) {
  val value: String = paths.mkString(":")
}