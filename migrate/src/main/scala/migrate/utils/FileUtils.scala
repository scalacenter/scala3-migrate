package migrate.utils

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.util.Try

import migrate.AbsolutePath

private[migrate] object FileUtils {
  final def listFiles(base: AbsolutePath): Seq[AbsolutePath] =
    listFiles(base.toFile, true).map(AbsolutePath.from).filter(_.value.endsWith(".scala"))

  final def writeFile(file: AbsolutePath, content: String): Try[Unit] =
    Try {
      val bw = new BufferedWriter(new FileWriter(file.toFile))
      bw.write(content)
      bw.close()
    }

  private def listFiles(base: File, recursively: Boolean = true): Seq[File] = Try {
    def listDir(dir: File): List[File] =
      if (dir.isDirectory) {
        Option(dir.listFiles).map(_.toList.flatMap(f => if (recursively) listDir(f) else List(f))).getOrElse(List())
      } else {
        List(dir)
      }
    listDir(base).filter(_.isFile).sorted
  }.getOrElse(List())

  def read(path: AbsolutePath, charset: Charset = StandardCharsets.UTF_8): String =
    new String(Files.readAllBytes(path.toNio), charset)

  def tryRead(path: AbsolutePath): Try[String] = Try(read(path))

}
