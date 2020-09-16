package utils

import java.io.File

import domain.AbsolutePath

object FileUtils {
  final def listFiles(base: AbsolutePath): Seq[AbsolutePath] = {
    listFiles(base.toFile, true).map(AbsolutePath.from)
  }

  private def listFiles(base: File, recursive: Boolean = true): Seq[File] = {
    val files = base.listFiles
    val result = files.filter(_.isFile)
    result ++
      files
        .filter(_.isDirectory)
        .filter(_ => recursive)
        .flatMap(listFiles(_, recursive))
  }
}
