package migrate.internal

import scala.jdk.OptionConverters._
import scala.util.Try

import migrate.interfaces.CompilationUnit
import migrate.interfaces.Logger
import migrate.interfaces.Scala3Compiler
import migrate.utils.ScalaExtensions._
import scalafix.interfaces._

sealed trait FileMigrationState {
  val evaluation: ScalafixFileEvaluation

  def source: AbsolutePath = AbsolutePath.from(evaluation.getEvaluatedFile).get

  def patches: Seq[ScalafixPatch] = evaluation.getPatches().toSeq

  def previewAllPatches(): Try[CompilationUnit] =
    evaluation
      .previewPatches()
      .map { content =>
        new CompilationUnit(source.value, content)
      }
      .toScala
      .toTry {
        val msg =
          s"""|Cannot apply patch on file $source because:
              |${evaluation.getErrorMessage.toScala.getOrElse("Unexpected error")}
              |""".stripMargin
        new ScalafixException(msg)
      }

  def previewPatches(patches: Seq[ScalafixPatch]): Try[CompilationUnit] =
    evaluation
      .previewPatches(patches.toArray)
      .map { content =>
        new CompilationUnit(source.value, content)
      }
      .toScala
      .toTry {
        val msg =
          s"""|Cannot apply patch on file $source because:
              |${evaluation.getErrorMessage.toScala.getOrElse("Unexpected error")}
              |""".stripMargin
        new ScalafixException(msg)
      }
}

object FileMigrationState {
  case class Initial(evaluation: ScalafixFileEvaluation) extends FileMigrationState {
    def migrate(compiler: Scala3Compiler, logger: Logger): Try[FileMigrationState.FinalState] =
      new FileMigration(this, compiler, logger).migrate()

    def success(necessaryPatches: Seq[ScalafixPatch]): FinalState = FinalState(evaluation, necessaryPatches)

  }
  case class FinalState(evaluation: ScalafixFileEvaluation, necessaryPatches: Seq[ScalafixPatch])
      extends FileMigrationState {
    def newFileContent: Try[String] = previewPatches(necessaryPatches).map(_.content)
  }

}
