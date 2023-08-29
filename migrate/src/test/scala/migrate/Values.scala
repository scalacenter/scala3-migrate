package migrate

import migrate.interfaces.Scala3Compiler
import migrate.internal.AbsolutePath
import migrate.internal.Classpath
import migrate.test.BuildInfo
import migrate.utils.ScalafixService

object Values {

  val unmanaged: Seq[AbsolutePath]       = BuildInfo.unmanagedSources.map(AbsolutePath.from)
  val managed: Seq[AbsolutePath]         = BuildInfo.managedSources.map(AbsolutePath.from)
  val input: AbsolutePath                = AbsolutePath.from(BuildInfo.input)
  val output: AbsolutePath               = AbsolutePath.from(BuildInfo.output)
  val scala2Classpath: Classpath         = Classpath.from(BuildInfo.scala2Classpath).get
  val semanticdbTargetRoot: AbsolutePath = AbsolutePath.from(BuildInfo.semanticdbPath)
  val scala2CompilerOptions              = BuildInfo.scala2CompilerOptions
  val scala3Classpath: Classpath         = Classpath.from(BuildInfo.scala3Classpath).get
  val scala3CompilerOptions              = BuildInfo.scala3CompilerOptions
  val scala3ClassDirectory: AbsolutePath = AbsolutePath.from(BuildInfo.scala3ClassDirectory)

  lazy val scalafixSrv: ScalafixService =
    ScalafixService.from(scala2CompilerOptions, scala2Classpath, semanticdbTargetRoot, PrintLogger).get
  val scalaMigrat = new Scala3Migrate(scalafixSrv, PrintLogger)
  val scala3Compiler: Scala3Compiler =
    scalaMigrat.setupScala3Compiler(scala3Classpath, scala3ClassDirectory, scala3CompilerOptions).get
}
