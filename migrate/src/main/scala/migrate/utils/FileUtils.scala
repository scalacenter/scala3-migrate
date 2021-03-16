package migrate.utils

import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.util.Try

import migrate.internal.AbsolutePath

private[migrate] object FileUtils {

  final def writeFile(file: AbsolutePath, content: String): Try[Unit] =
    Try {
      val bw = new BufferedWriter(new FileWriter(file.toFile))
      bw.write(content)
      bw.close()
    }

  def read(path: AbsolutePath, charset: Charset = StandardCharsets.UTF_8): String =
    new String(Files.readAllBytes(path.toNio), charset)

  def tryRead(path: AbsolutePath): Try[String] = Try(read(path))

}
