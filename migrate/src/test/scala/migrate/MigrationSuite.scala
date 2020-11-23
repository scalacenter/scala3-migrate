package migrate

import scala.util.Failure
import scala.util.Try

import migrate.internal.FileMigrationState
import migrate.test.BuildInfo
import migrate.utils.FileUtils._
import migrate.utils.ScalaExtensions._
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrationSuite extends AnyFunSuiteLike with DiffAssertions {

  val sources: Seq[AbsolutePath]         = BuildInfo.sources.map(AbsolutePath.from)
  val input: AbsolutePath                = AbsolutePath.from(BuildInfo.input)
  val output: AbsolutePath               = AbsolutePath.from(BuildInfo.output)
  val scala2Classpath: Classpath         = Classpath.from(BuildInfo.scala2Classpath).get
  val semanticdbTargetRoot: AbsolutePath = AbsolutePath.from(BuildInfo.semanticdbPath)
  val scala2CompilerOptions              = BuildInfo.scala2CompilerOptions.toSeq
  val toolClasspath: Classpath           = Classpath.from(BuildInfo.toolClasspath).get
  val scala3Classpath: Classpath         = Classpath.from(BuildInfo.scala3Classpath).get
  val scala3CompilerOptions              = BuildInfo.scala3CompilerOptions.toSeq
  val scala3ClassDirectory: AbsolutePath = AbsolutePath.from(BuildInfo.scala3ClassDirectory)

  sources.foreach { inputFile =>
    test(s"${inputFile.getName}") {
      val migrateResult = Main
        .previewMigration(
          sources = Seq(inputFile),
          scala2Classpath = scala2Classpath,
          scala2CompilerOptions = scala2CompilerOptions,
          toolClasspath = toolClasspath,
          targetRoot = semanticdbTargetRoot,
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
    migratedFiles.get(filetoMigrate).toTry(new Exception(s"Cannot find $filetoMigrate")).flatMap {
      case FileMigrationState.Failed(_, cause) => Failure(cause)
      case f: FileMigrationState.Succeeded     => f.newFileContent
    }

}
