package migrate.utils

import scala.jdk.CollectionConverters._
import scala.util.Try

import buildinfo.BuildInfo
import coursier._
import migrate.AbsolutePath
import migrate.Classpath
import migrate.utils.ScalaExtensions.OptionalExtension
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixEvaluation

final case class ScalafixService(
  scalafix: Scalafix,
  sources: Seq[AbsolutePath],
  compilerOptions: Seq[String],
  classpath: Classpath,
  targetRootSemantic: AbsolutePath,
  toolClasspath: Classpath
) {
  import ScalafixService._
  lazy val scalafixClassLoader: ClassLoader = scalafix.getClass().getClassLoader()

  def inferTypesAndImplicits(): Try[ScalafixEvaluation] = evaluate(addExplicitResultTypesAndImplicits)

  def fixSyntaxForScala3(): Try[ScalafixEvaluation] =
    evaluate(fixSyntaxRules)

  def fixInPlace(eval: ScalafixEvaluation): Unit =
    if (eval.isSuccessful) {
      val filesEvaluated = eval.getFileEvaluations.toSeq
      filesEvaluated.foreach { oneFile =>
        val absPath = AbsolutePath.from(oneFile.getEvaluatedFile).get
        if (oneFile.isSuccessful) {
          oneFile.previewPatchesAsUnifiedDiff().asScala match {
            case None => scribe.info(s"Nothing to fix in $absPath)")
            case Some(_) => {
              oneFile.applyPatches()
              scribe.info(s"Syntax fixed for $absPath)")
            }
          }
        } else {
          val errorMsg = oneFile.getErrorMessage.asScala.getOrElse("Unknown Error")
          scribe.info(s"Failed to run scalafix with ${fixSyntaxRules.mkString(", ")} on $absPath because $errorMsg")
        }
      }
    } else {
      val errorMsg = eval.getErrorMessage.asScala.getOrElse("Unknown Error")
      scribe.info(s"Failed to run scalafix with ${fixSyntaxRules.mkString(", ")} because $errorMsg")

    }

  private def evaluate(rules: Seq[String]): Try[ScalafixEvaluation] = Try {
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
  private lazy val internalRules = Classpath.from(BuildInfo.toolClasspath)
  private lazy val externalRules = getClassPathforRewriteRules()

  val fixSyntaxRules: Seq[String] = Seq(
    "ProcedureSyntax",
    "fix.scala213.ConstructorProcedureSyntax",
    "fix.scala213.ExplicitNullaryEtaExpansion",
    "fix.scala213.ParensAroundLambda",
    "fix.scala213.ExplicitNonNullaryApply",
    "fix.scala213.Any2StringAdd"
  )
  val addExplicitResultTypesAndImplicits: Seq[String] = Seq("InferTypes", "ExplicitImplicits")

  def from(
    sources: Seq[AbsolutePath],
    compilerOptions: Seq[String],
    classpath: Classpath,
    targetRootSemantic: AbsolutePath
  ): Try[ScalafixService] =
    for {
      scalafix      <- scalafix
      internalRules <- internalRules
      externalRules <- externalRules
    } yield ScalafixService(
      scalafix,
      sources,
      compilerOptions,
      classpath,
      targetRootSemantic,
      internalRules ++ externalRules
    )

  private def getClassPathforRewriteRules(): Try[Classpath] =
    Try {
      val paths1 = downloadDependecies(dep"org.scala-lang:scala-rewrites_2.13:0.1.2")
      val paths2 = downloadDependecies(dep"com.sandinh:scala-rewrites_2.13:0.1.10-sd")
      Classpath((paths1 ++ paths2): _*)
    }

  def downloadDependecies(dep: Dependency): Seq[AbsolutePath] =
    Fetch()
      .addDependencies(dep)
      .run()
      .map(AbsolutePath.from)

}
