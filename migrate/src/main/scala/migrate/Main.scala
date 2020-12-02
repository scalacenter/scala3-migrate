package migrate

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import compiler.interfaces.CompilationUnit
import compiler.interfaces.Scala3Compiler
import migrate.internal._
import migrate.utils.FileUtils
import migrate.utils.ScalaExtensions._
import migrate.utils.ScalafixService
import migrate.utils.Timer._

object Main {

  def previewMigration(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    scala2Classpath: Classpath,
    scala2CompilerOptions: Seq[String],
    targetRoot: AbsolutePath,
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Map[AbsolutePath, FileMigrationState.FinalState]] = {
    val migrationRules = Seq("MigrationRule", "ExplicitImplicits")
    unmanagedSources.foreach(f => scribe.info(s"Migrating $f"))
    for {
      scalafixSrv <- ScalafixService.from(unmanagedSources, scala2CompilerOptions, scala2Classpath, targetRoot)
      compiler    <- setupScala3Compiler(scala3Classpath, scala3ClassDirectory, scala3CompilerOptions)
      initialFileToMigrate <-
        buildMigrationFiles(unmanagedSources, migrationRules, scalafixSrv)
      _            <- compileInScala3(initialFileToMigrate, managedSources, compiler)
      migratedFiles = initialFileToMigrate.map(f => (f.source, f.migrate(compiler))).toMap
    } yield migratedFiles
  }

  def migrate(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    scala2Classpath: Classpath,
    scala2CompilerOptions: Seq[String],
    targetRoot: AbsolutePath,
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Unit] =
    for {
      migratedFiles <- previewMigration(
                         unmanagedSources,
                         managedSources,
                         scala2Classpath,
                         scala2CompilerOptions,
                         targetRoot,
                         scala3Classpath,
                         scala3CompilerOptions,
                         scala3ClassDirectory
                       )
      (success, failures) = migratedFiles.toSeq.partition { case (_, migrated) => migrated.isSuccess }
      _ <- success.map { case (file, migrated: FileMigrationState.Succeeded) =>
             migrated.newFileContent.flatMap(FileUtils.writeFile(file, _))
           }.sequence
      _ = success.foreach { case (file, _) => scribe.info(s"${file.value} has been successfully migrated") }
      _ = failures.foreach { case (file, FileMigrationState.Failed(_, cause)) =>
            scribe.info(s"${file.value} has not been migrated because ${cause.getMessage()}")
          }
    } yield ()

  private def setupScala3Compiler(
    classpath: Classpath,
    classDirectory: AbsolutePath,
    scala3CompilerOptions: Seq[String]
  ): Try[Scala3Compiler] = {
    // It's easier no to deal with semanticdb option, since we don't need semanticdb files
    val modified           = scala3CompilerOptions.filterNot(_ == "-Ysemanticdb")
    val scala3CompilerArgs = modified.toArray ++ Array("-classpath", classpath.value, "-d", classDirectory.value)
    Try {
      Scala3Compiler.setup(scala3CompilerArgs)
    }
  }

  private def compileInScala3(
    migrationFiles: Seq[FileMigrationState],
    managedSources: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Try[Unit] =
    for {
      cuUnmanagedSources <- migrationFiles.map(_.previewAllPatches()).sequence
      cuManagedSources    = managedSources.map(path => new CompilationUnit(path.value, FileUtils.read(path)))
      _ <- timeAndLog(Try(compiler.compile(cuManagedSources.toList ++ cuUnmanagedSources.toList))) {
             case (finiteDuration, Success(_)) =>
               scribe.info(s"Succefully compiled with scala 3 in $finiteDuration")
             case (_, Failure(e)) =>
               scribe.info(s"""|Compilation with scala 3 failed because:
                               |Cause: ${e.getMessage}""".stripMargin)
           }

    } yield ()

  private def buildMigrationFiles(
    unmanagedSources: Seq[AbsolutePath],
    rules: Seq[String],
    scalafixSrv: ScalafixService
  ): Try[Seq[FileMigrationState.Initial]] =
    for {
      fileEvaluations <-
        timeAndLog(scalafixSrv.evaluate(rules)) {
          case (duration, Success(files)) =>
            val fileEvaluationsSeq = files.getFileEvaluations().toSeq
            val patchesCount       = fileEvaluationsSeq.map(_.getPatches().size).sum
            scribe.info(s"Found ${patchesCount} patch candidate(s) in ${unmanagedSources.size} file(s)after $duration")
          case (_, Failure(e)) =>
            scribe.info(s"""|Failed inferring types 
                            |Cause ${e.getMessage()}""".stripMargin)
        }
      fileEvaluationMap <- fileEvaluations
                             .getFileEvaluations()
                             .toSeq
                             .map(e => AbsolutePath.from(e.getEvaluatedFile()).map(file => file -> e))
                             .sequence
                             .map(_.toMap)
      fileToMigrate <-
        unmanagedSources.map(src => fileEvaluationMap.get(src).map(FileMigrationState.Initial).toTry).sequence
    } yield fileToMigrate

}
