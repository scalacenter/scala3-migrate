package domain

import java.io.File

case class Classpath(paths: AbsolutePath*) {
  val value: String = paths.mkString(File.pathSeparator)
}