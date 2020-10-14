package migrate.utils

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files

import migrate.AbsolutePath

import scala.util.Try

private[migrate] object FileUtils {
  final def listFiles(base: AbsolutePath): Seq[AbsolutePath] = {
    listFiles(base.toFile, true).map(AbsolutePath.from)
  }

  private def listFiles(base: File, recursive: Boolean = true): Seq[File] = {
    val (files, directories) = base.listFiles().toSeq.partition(_.isFile)
    files ++ {
      if (recursive) directories.flatMap(listFiles(_, recursive))
      else Seq.empty
    }
  }

  def read(path: AbsolutePath, charset: Charset = StandardCharsets.UTF_8): String =
    new String(Files.readAllBytes(path.toNio), charset)
  
  def tryRead(path: AbsolutePath): Try[String] = Try { read(path) }
}
