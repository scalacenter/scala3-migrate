package migrate.utils

import scala.jdk.CollectionConverters._
import scala.util.Try

import buildinfo.BuildInfo
import migrate.AbsolutePath
import migrate.Classpath
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixEvaluation

final class ScalafixService(
  scalafix: Scalafix,
  sources: Seq[AbsolutePath],
  compilerOptions: Seq[String],
  classpath: Classpath,
  targetRootSemantic: AbsolutePath,
  toolClasspath: Classpath
) {
  lazy val scalafixClassLoader: ClassLoader = scalafix.getClass().getClassLoader()

  def evaluate(rules: Seq[String]): Try[ScalafixEvaluation] = Try {
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

  def from(
    sources: Seq[AbsolutePath],
    compilerOptions: Seq[String],
    classpath: Classpath,
    targetRootSemantic: AbsolutePath
  ): Try[ScalafixService] =
    for {
      scalafix      <- Try(Scalafix.fetchAndClassloadInstance("2.13"))
      toolClasspath <- Classpath.from(BuildInfo.toolClasspath)
    } yield new ScalafixService(scalafix, sources, compilerOptions, classpath, targetRootSemantic, toolClasspath)

}
