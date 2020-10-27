package migrate

import java.io.File
import java.net.URLClassLoader

import scala.util.Try

import migrate.utils.ScalaExtensions._

case class Classpath(paths: AbsolutePath*) {
  val value: String = paths.mkString(File.pathSeparator)

  def toUrlClassLoader(parent: ClassLoader): URLClassLoader = {
    val urls = paths.map(_.toNio.toUri.toURL).toArray
    new URLClassLoader(urls, parent)
  }

  def :+(newPath: AbsolutePath): Classpath = Classpath((paths :+ newPath): _*)
}
object Classpath {
  def from(value: String): Try[Classpath] = {
    val absolutePaths = value.split(java.io.File.pathSeparator).toList.map(AbsolutePath.from).sequence
    absolutePaths.map(paths => Classpath(paths: _*))
  }
}
