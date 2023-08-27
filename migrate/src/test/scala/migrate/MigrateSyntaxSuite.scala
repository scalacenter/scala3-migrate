package migrate

import scala.jdk.OptionConverters._
import scala.tools.nsc.io.File

import migrate.Values._
import migrate.internal.AbsolutePath
import migrate.utils.FileUtils._
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.interfaces.ScalafixEvaluation
import scalafix.testkit.DiffAssertions

class MigrateSyntaxSuite extends AnyFunSuiteLike with DiffAssertions {

  val migrateFiles: Seq[AbsolutePath] =
    unmanaged.filter(_.value.contains(s"${File.separator}migrateSyntax${File.separator}"))
  migrateFiles.foreach { inputFile =>
    test(s"${inputFile.getName}") {
      val fixSyntaxResult = scalaMigrat
        .previewSyntaxMigration(Seq(inputFile))
        .get

      val preview       = previewSyntaxFix(inputFile, fixSyntaxResult).get
      val relative      = inputFile.relativize(input).get
      val outputFile    = output.child(relative)
      val outputContent = read(outputFile)

      assertNoDiff(preview, outputContent)
    }
  }

  def previewSyntaxFix(path: AbsolutePath, evaluation: ScalafixEvaluation): Option[String] =
    evaluation.getFileEvaluations.toList
      .find(_.getEvaluatedFile == path.toNio)
      .flatMap(_.previewPatches.toScala)

}
