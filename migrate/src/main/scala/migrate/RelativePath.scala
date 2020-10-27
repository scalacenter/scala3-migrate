package migrate

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import scala.util.Try

case class RelativePath private (value: String) {
  require(!toPath.isAbsolute, s"$toPath is not relative!")

  def toPath: Path              = Paths.get(value)
  def toFile: File              = new File(value)
  def getName: PathName         = PathName(toPath.getFileName.toString)
  def getParent: RelativePath   = RelativePath(toPath.getParent.toString)
  override def toString: String = value

}

object RelativePath {
  def from(value: String): Try[RelativePath] = Try(RelativePath(value))
  def from(path: Path): Try[RelativePath]    = Try(RelativePath(path.toString))
}
