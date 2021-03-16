package migrate

import scala.tools.nsc.io.File
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import migrate.Values._
import migrate.internal.FileMigrationState
import migrate.utils.FileUtils._
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrationSuite extends AnyFunSuiteLike with DiffAssertions {
  val migrateFiles: Seq[AbsolutePath] = unmanaged.filter(_.value.contains(s"${File.separator}migrate${File.separator}"))
  migrateFiles.foreach { inputFile =>
    test(s"${inputFile.getName}") {
      val migrateResult = Values.scalaMigrat
        .previewMigration(
          unmanagedSources = Seq(inputFile),
          managedSources = managed,
          scala3Classpath = scala3Classpath,
          scala3CompilerOptions = scala3CompilerOptions,
          scala3ClassDirectory = scala3ClassDirectory
        )
        .get

      val preview       = previewMigration(inputFile, migrateResult).get
      val relative      = inputFile.relativize(input).get
      val outputFile    = output.child(relative)
      val outputContent = read(outputFile)

      assertNoDiff(preview, outputContent)
    }
  }

  def previewMigration(
    filetoMigrate: AbsolutePath,
    migratedFiles: Map[AbsolutePath, FileMigrationState.FinalState]
  ): Try[String] =
    migratedFiles
      .get(filetoMigrate)
      .map {
        case FileMigrationState.Failed(_, cause) => Failure(cause)
        case f: FileMigrationState.Succeeded     => f.newFileContent
      }
      .getOrElse(Success(read(filetoMigrate)))

}
