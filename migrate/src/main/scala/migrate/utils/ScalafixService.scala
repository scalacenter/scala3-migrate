package migrate.utils

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.Try

import coursier._
import migrate.buildinfo.BuildInfo
import migrate.interfaces.Logger
import migrate.internal.AbsolutePath
import migrate.internal.Classpath
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixEvaluation

final class ScalafixService(
  scalafix: Scalafix,
  compilerOptions: Seq[String],
  classpath: Classpath,
  targetRootSemantic: AbsolutePath,
  toolClasspath: Classpath,
  baseDirectory: AbsolutePath,
  logger: Logger
) {
  import ScalafixService._
  lazy val scalafixClassLoader: ClassLoader = scalafix.getClass().getClassLoader()

  def inferTypesAndImplicits(unmanagedSources: Seq[AbsolutePath]): Try[ScalafixEvaluation] =
    evaluate(addExplicitResultTypesAndImplicits, unmanagedSources)

  def fixSyntaxForScala3(unmanagedSources: Seq[AbsolutePath]): Try[ScalafixEvaluation] =
    evaluate(fixSyntaxRules, unmanagedSources)

  def fixInPlace(eval: ScalafixEvaluation): Unit =
    if (eval.isSuccessful) {
      val filesEvaluated = eval.getFileEvaluations.toSeq
      filesEvaluated.foreach { evaluation =>
        val file         = AbsolutePath.from(evaluation.getEvaluatedFile).get
        val relativePath = file.relativize(baseDirectory).getOrElse(file)
        if (evaluation.isSuccessful) {
          evaluation.previewPatchesAsUnifiedDiff.toScala match {
            case None =>
            case Some(_) =>
              evaluation.applyPatches()
              logger.info(s"Applied ${Format.plural(evaluation.getPatches.size, "patch", "patches")} in $relativePath")
          }
        } else {
          val errorMsg = evaluation.getErrorMessage.toScala.getOrElse("unknown error")
          logger.error(s"Failed to fix syntax in $relativePath because: $errorMsg.")
        }
      }
    } else {
      val errorMsg = eval.getErrorMessage.toScala.getOrElse("unknown error")
      logger.error(s"Failed to fix syntax because of $errorMsg")
    }

  private def evaluate(rules: Seq[String], sources: Seq[AbsolutePath]): Try[ScalafixEvaluation] = Try {
    val classpathWithTargetSemantic = classpath :+ targetRootSemantic
    val args = scalafix
      .newArguments()
      .withRules(rules.asJava)
      .withPaths(sources.map(_.toNio).asJava)
      .withClasspath(classpathWithTargetSemantic.paths.map(_.toNio).asJava)
      .withScalacOptions(compilerOptions.asJava)
      .withToolClasspath(toolClasspath.toUrlClassLoader(scalafixClassLoader))
    args.evaluate()
  }
}

object ScalafixService {
  private lazy val scalafix      = Try(Scalafix.fetchAndClassloadInstance("2.13"))
  private lazy val internalRules = getClassPathforMigrateRules()
  private lazy val externalRules = getClassPathforRewriteRules()

  val fixSyntaxRules: Seq[String] = Seq(
    "ProcedureSyntax",
    "fix.scala213.ExplicitNullaryEtaExpansion",
    "fix.scala213.ParensAroundLambda",
    "fix.scala213.ExplicitNonNullaryApply",
    "fix.scala213.Any2StringAdd",
    "ExplicitResultTypes"
  )
  val addExplicitResultTypesAndImplicits: Seq[String] = Seq("MigrationRule")

  def from(
    compilerOptions: Seq[String],
    classpath: Classpath,
    targetRootSemantic: AbsolutePath,
    baseDirectory: AbsolutePath,
    logger: Logger): Try[ScalafixService] =
    for {
      scalafix      <- scalafix
      internalRules <- internalRules
      externalRules <- externalRules
    } yield new ScalafixService(
      scalafix,
      compilerOptions,
      classpath,
      targetRootSemantic,
      internalRules ++ externalRules,
      baseDirectory,
      logger)

  private def getClassPathforRewriteRules(): Try[Classpath] =
    Try {
      val paths1 = downloadDependecies(dep"org.scala-lang:scala-rewrites_2.13:0.1.5")
      val paths2 = downloadDependecies(dep"com.sandinh:scala-rewrites_2.13:1.1.0-M1")
      Classpath((paths1 ++ paths2): _*)
    }

  private def getClassPathforMigrateRules(): Try[Classpath] = {
    val dependency =
      Dependency(Module(Organization("ch.epfl.scala"), ModuleName(s"scala3-migrate-rules_2.13")), BuildInfo.version)

    Try {
      val paths1 = downloadDependecies(dependency)
      Classpath(paths1: _*)
    }
  }

  def downloadDependecies(dep: Dependency): Seq[AbsolutePath] =
    Fetch()
      .addDependencies(dep)
      .run()
      .map(AbsolutePath.from)

}
