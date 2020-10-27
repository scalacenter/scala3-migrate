package migrate

import migrate.test.BuildInfo
import migrate.utils.FileUtils._
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrationSuite extends AnyFunSuiteLike with DiffAssertions {

  val input: AbsolutePath                = AbsolutePath.from(BuildInfo.input)
  val output: AbsolutePath               = AbsolutePath.from(BuildInfo.output)
  val workspace: AbsolutePath            = AbsolutePath.from(BuildInfo.workspace)
  val scala2Classpath: Classpath         = Classpath.from(BuildInfo.scala2Classpath).get
  val semanticdbTargetRoot: AbsolutePath = AbsolutePath.from(BuildInfo.semanticdbPath)
  val scala2CompilerOptions              = BuildInfo.scala2CompilerOptions.toSeq
  val toolClasspath: Classpath           = Classpath.from(BuildInfo.toolClasspath).get
  val scala3Classpath: Classpath         = Classpath.from(BuildInfo.scala3Classpath).get
  val scala3CompilerOptions              = BuildInfo.scala3CompilerOptions.toSeq
  val scala3ClassDirectory: AbsolutePath = AbsolutePath.from(BuildInfo.scala3ClassDirectory)

  listFiles(input).foreach { inputFile =>
    test(s"${inputFile.getName}") {
      val scala2ClasspathWithSemanticdb = scala2Classpath :+ semanticdbTargetRoot

      val preview = Main
        .previewMigration(
          workspace,
          inputFile,
          scala2ClasspathWithSemanticdb,
          scala2CompilerOptions,
          toolClasspath,
          scala3Classpath,
          scala3CompilerOptions,
          scala3ClassDirectory
        )
        .get

      val relative      = inputFile.relativize(input).get
      val outputFile    = output.child(relative)
      val outputContent = read(outputFile)

      assertNoDiff(preview, outputContent)
    }
  }

}
