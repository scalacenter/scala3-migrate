package utils

import java.io.File
import java.nio.file.{Path, Paths}

import scala.util.Try

case class AbsolutePath private(value: String) {
  require(toPath.isAbsolute, s"$toPath is not absolute!")

  def toPath: Path = Paths.get(value)
  def toFile: File = new File(value)
  def relativize(parent: AbsolutePath): Try[RelativePath] =
    Try(parent.toPath.relativize(toPath)).flatMap(RelativePath.from)

  def child(relativePath: RelativePath): AbsolutePath = AbsolutePath(Paths.get(value, relativePath.value).toString)
  def child(pathName: PathName): AbsolutePath = AbsolutePath(Paths.get(value, pathName.value).toString)
  def getName: PathName = PathName(toPath.getFileName.toString)
  def getParent: AbsolutePath = AbsolutePath(toPath.getParent.toString)
  override def toString: String = value
}

object AbsolutePath {
  def from(value: String): Try[AbsolutePath] = Try(AbsolutePath(value))
  def from(path: Path): Try[AbsolutePath] = Try(AbsolutePath(path.toString))
  def from(file: File): Try[AbsolutePath] = Try(AbsolutePath(file.toString))
}