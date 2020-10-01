package migrate.internal

import interfaces.CompilationUnit
import scalafix.interfaces._
import migrate.utils.FileUtils
import scala.util.{Try, Failure, Success}
import migrate.utils.ScalaExtensions._
import migrate.AbsolutePath

private[migrate] trait MigrationFile {
  def source: AbsolutePath
  def patches: Seq[ScalafixPatch]
  def previewAllPatches(): Try[CompilationUnit]
  def previewPatches(patches: Seq[ScalafixPatch]): Try[CompilationUnit]
}

private[migrate] object MigrationFile {
  def apply(source: AbsolutePath, evaluation: Option[ScalafixFileEvaluation]): MigrationFile = {
    evaluation
      .map(ScalafixMigrationFile(source, _))
      .getOrElse(DefaultMigrationFile(source))
  }
}

private[migrate] case class ScalafixMigrationFile(source: AbsolutePath, evaluation: ScalafixFileEvaluation) extends MigrationFile {
  val patches: Seq[ScalafixPatch] = evaluation.getPatches().toSeq

  def previewAllPatches(): Try[CompilationUnit] = {
    evaluation.previewPatches()
      .map { content => 
        new CompilationUnit(source.value, content)
      }
      .asScala
      .map(Success(_))
      .getOrElse {
        Failure(new ScalafixException(s"Cannot apply patch on file $source"))
      }
  }

  def previewPatches(patches: Seq[ScalafixPatch]): Try[CompilationUnit] = {
    evaluation.previewPatches(patches.toArray)
      .map { content => 
        new CompilationUnit(source.value, content)
      }
      .asScala
      .map(Success(_))
      .getOrElse {
        Failure(new ScalafixException(s"Cannot apply patch on file $source"))
      }
  }
}

private[migrate] case class DefaultMigrationFile(source: AbsolutePath) extends MigrationFile {
  val patches: Seq[ScalafixPatch] = Seq.empty

  def previewAllPatches(): Try[CompilationUnit] = Try {
    val content = FileUtils.read(source)
    new CompilationUnit(source.value, content)
  }

  def previewPatches(patches: Seq[ScalafixPatch]): Try[CompilationUnit] = previewAllPatches()
}
