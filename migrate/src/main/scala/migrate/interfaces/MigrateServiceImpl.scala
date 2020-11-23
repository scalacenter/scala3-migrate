package migrate.interfaces

import java.nio.file.Path
import java.{ util => jutil }

import scala.jdk.CollectionConverters._

import migrate.AbsolutePath
import migrate.Classpath
import migrate.Main
import migrate.internal.FileMigrationState
import migrate.utils.FileUtils
import migrate.utils.ScalaExtensions._

final class MigrateImpl() extends Migrate {

  override def migrate(
    sources: jutil.List[Path],
    workspace: Path,
    scala2Cp: jutil.List[Path],
    scala2CompilerOptions: jutil.List[String],
    toolCp: jutil.List[Path],
    scala3Cp: jutil.List[Path],
    scala3CompilerOptions: jutil.List[String],
    scala3ClassDirectory: Path
  ): Unit = {

    val sourcesAbs = sources.asScala.toSeq.map(AbsolutePath.from(_).get)
    val sourceAbs  = AbsolutePath.from(workspace).get

    val scala2Classpath = Classpath(scala2Cp.asScala.toList.map(m => AbsolutePath.from(m).get).toList: _*)
    val toolClasspath   = Classpath(toolCp.asScala.toList.map(AbsolutePath.from(_).get).toList: _*)
    val scala3Classpath = Classpath(scala3Cp.asScala.toList.map(AbsolutePath.from(_).get).toList: _*)

    val scala3ClassDirectoryAbs = AbsolutePath.from(scala3ClassDirectory).get

    val fileMigratedTry = Main
      .migrate(
        sources = sourcesAbs,
        workspace = sourceAbs,
        scala2Classpath = scala2Classpath,
        scala2CompilerOptions = scala2CompilerOptions.asScala.toList,
        toolClasspath = toolClasspath,
        scala3Classpath = scala3Classpath,
        scala3CompilerOptions = scala3CompilerOptions.asScala.toList,
        scala3ClassDirectory = scala3ClassDirectoryAbs
      )

    (for {
      fileMigrated       <- fileMigratedTry
      (success, failures) = fileMigrated.toSeq.partition { case (_, migrated) => migrated.isSuccess }
      _ <- success.map { case (file, migrated: FileMigrationState.Succeeded) =>
             migrated.newFileContent.flatMap(FileUtils.writeFile(file, _))
           }.sequence
      _ = success.foreach { case (file, _) => scribe.info(s"${file.value} has been successfully migrated") }
      _ = failures.foreach { case (file, FileMigrationState.Failed(_, cause)) =>
            scribe.info(s"${file.value} has not been migrated because ${cause.getMessage()}")
          }
    } yield ()).get
  }

}
