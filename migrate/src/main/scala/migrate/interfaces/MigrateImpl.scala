package migrate.interfaces

import java.nio.file.Path
import java.{ util => jutil }

import scala.jdk.CollectionConverters._

import migrate.AbsolutePath
import migrate.Classpath
import migrate.Main

final class MigrateImpl() extends Migrate {

  override def migrate(
    unmanagedSources: jutil.List[Path],
    managedSources: jutil.List[Path],
    targetRoot: Path,
    scala2Cp: jutil.List[Path],
    scala2CompilerOptions: jutil.List[String],
    toolCp: jutil.List[Path],
    scala3Cp: jutil.List[Path],
    scala3CompilerOptions: jutil.List[String],
    scala3ClassDirectory: Path
  ): Unit = {

    val unmanagedSourcesAbs = unmanagedSources.asScala.toSeq.map(AbsolutePath.from(_).get)
    val managedSourcesAbs   = managedSources.asScala.toSeq.map(AbsolutePath.from(_).get)
    val targetRootAbs       = AbsolutePath.from(targetRoot).get

    val scala2Classpath = Classpath(scala2Cp.asScala.toList.map(m => AbsolutePath.from(m).get).toList: _*)
    val toolClasspath   = Classpath(toolCp.asScala.toList.map(AbsolutePath.from(_).get).toList: _*)
    val scala3Classpath = Classpath(scala3Cp.asScala.toList.map(AbsolutePath.from(_).get).toList: _*)

    val scala3ClassDirectoryAbs = AbsolutePath.from(scala3ClassDirectory).get

    Main
      .migrate(
        unmanagedSources = unmanagedSourcesAbs,
        managedSources = managedSourcesAbs,
        scala2Classpath = scala2Classpath,
        targetRoot = targetRootAbs,
        scala2CompilerOptions = scala2CompilerOptions.asScala.toList,
        toolClasspath = toolClasspath,
        scala3Classpath = scala3Classpath,
        scala3CompilerOptions = scala3CompilerOptions.asScala.toList,
        scala3ClassDirectory = scala3ClassDirectoryAbs
      )
      .get

  }

}
