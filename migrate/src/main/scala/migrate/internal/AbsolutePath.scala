package migrate.internal

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import scala.util.Try

case class AbsolutePath private (value: String) {
  require(toNio.isAbsolute, s"$toNio is not absolute!")

  def toNio: Path  = Paths.get(value)
  def toFile: File = new File(value)
  def relativize(parent: AbsolutePath): Try[RelativePath] =
    Try(parent.toNio.relativize(toNio)).flatMap(RelativePath.from)

  def child(relativePath: RelativePath): AbsolutePath = AbsolutePath(Paths.get(value, relativePath.value).toString)
  def child(pathName: PathName): AbsolutePath         = AbsolutePath(Paths.get(value, pathName.value).toString)
  def getName: PathName                               = PathName(toNio.getFileName.toString)
  def getParent: AbsolutePath                         = AbsolutePath(toNio.getParent.toString)
  override def toString: String                       = value
}

object AbsolutePath {
  def from(value: String): Try[AbsolutePath] = from(Paths.get(value))
  def from(path: Path): Try[AbsolutePath]    = Try(AbsolutePath(path.toString))
  def from(file: File): AbsolutePath         = AbsolutePath(file.toString)
}
