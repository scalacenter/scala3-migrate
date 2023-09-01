package migrate.internal

import scala.jdk.OptionConverters._
import scala.util.Try

import migrate.compiler.interfaces.CompilationUnit
import migrate.compiler.interfaces.Scala3Compiler
import migrate.interfaces.Logger
import migrate.utils.ScalaExtensions._
import scalafix.interfaces._

sealed trait FileMigrationState {
  val evaluation: ScalafixFileEvaluation
  def baseDirectory: AbsolutePath

  def source: AbsolutePath = AbsolutePath.from(evaluation.getEvaluatedFile).get
  def relativePath: String = source.relativize(baseDirectory).getOrElse(source).toString

  def patches: Seq[ScalafixPatch] = evaluation.getPatches().toSeq

  def previewAllPatches(): Try[CompilationUnit] =
    evaluation
      .previewPatches()
      .map(content => new CompilationUnit(source.value, content, source.toNio))
      .toScala
      .toTry {
        val error = evaluation.getErrorMessage.toScala.getOrElse("unknown error")
        val msg   = s"Cannot apply patch on file $relativePath because: $error"
        new ScalafixException(msg)
      }

  def previewPatches(patches: Seq[ScalafixPatch]): Try[CompilationUnit] =
    evaluation
      .previewPatches(patches.toArray)
      .map(content => new CompilationUnit(source.value, content, source.toNio))
      .toScala
      .toTry {
        val error = evaluation.getErrorMessage.toScala.getOrElse("unknown error")
        val msg   = s"Cannot apply patch on file $relativePath because: $error"
        new ScalafixException(msg)
      }
}

object FileMigrationState {
  case class Initial(evaluation: ScalafixFileEvaluation, baseDirectory: AbsolutePath) extends FileMigrationState {
    def migrate(compiler: Scala3Compiler, logger: Logger): Try[FileMigrationState.FinalState] =
      new FileMigration(this, compiler, logger).migrate()

    def success(necessaryPatches: Seq[ScalafixPatch]): FinalState =
      FinalState(evaluation, necessaryPatches, baseDirectory)

  }
  case class FinalState(
    evaluation: ScalafixFileEvaluation,
    necessaryPatches: Seq[ScalafixPatch],
    baseDirectory: AbsolutePath)
      extends FileMigrationState {
    def newFileContent: Try[String] = previewPatches(necessaryPatches).map(_.content)
  }

}
