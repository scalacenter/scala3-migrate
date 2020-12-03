package migrate

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
    ScalafixService.from(unmanaged, scala2CompilerOptions, scala2Classpath, semanticdbTargetRoot).get
  val scalaMigrat = new ScalaMigrat(scalafixSrv)
}
