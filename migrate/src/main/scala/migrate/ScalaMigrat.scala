package migrate

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import compiler.interfaces.CompilationUnit
import compiler.interfaces.Scala3Compiler
import migrate.interfaces.Lib
import migrate.interfaces.MigratedScalacOptions
import migrate.internal._
import migrate.utils.FileUtils
import migrate.utils.ScalaExtensions._
import migrate.utils.ScalaMigrateLogger
import migrate.utils.ScalafixService
import migrate.utils.Timer._
import scalafix.interfaces.ScalafixEvaluation

class ScalaMigrat(scalafixSrv: ScalafixService) {
  private val reporter = ScalaMigrateLogger

  def previewMigration(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Map[AbsolutePath, FileMigrationState.FinalState]] = {
    unmanagedSources.foreach(f => scribe.info(s"Migrating $f"))
    for {
      compiler               <- setupScala3Compiler(scala3Classpath, scala3ClassDirectory, scala3CompilerOptions)
      (scalaFiles, javaFiles) = unmanagedSources.partition(_.value.endsWith("scala"))
      initialFileToMigrate <-
        buildMigrationFiles(scalaFiles)
      _ <- compileInScala3(initialFileToMigrate, javaFiles ++ managedSources, compiler)
      initialFileToMigrate <-
        buildMigrationFiles(unmanagedSources)
      _            <- compileInScala3(initialFileToMigrate, managedSources, compiler)
      migratedFiles = initialFileToMigrate.map(f => (f.source, f.migrate(compiler))).toMap
    } yield migratedFiles
  }

  def migrate(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Unit] =
    for {
      migratedFiles <-
        previewMigration(unmanagedSources, managedSources, scala3Classpath, scala3CompilerOptions, scala3ClassDirectory)
      (success, failures) = migratedFiles.toSeq.partition { case (_, migrated) => migrated.isSuccess }
      _ <- success.map { case (file, migrated: FileMigrationState.Succeeded) =>
             migrated.newFileContent.flatMap(FileUtils.writeFile(file, _))
           }.sequence
      _ = success.foreach { case (file, _) => scribe.info(s"${file.value} has been successfully migrated") }
      _ = failures.foreach { case (file, FileMigrationState.Failed(_, cause)) =>
            scribe.info(s"${file.value} has not been migrated because ${cause.getMessage()}")
          }
    } yield ()

  def previewPrepareMigration(unmanagedSources: Seq[AbsolutePath]): Try[ScalafixEvaluation] = {
    unmanagedSources.foreach(f => scribe.info(s"Fixing syntax of $f"))
    for {
      scalafixEval <- timeAndLog(scalafixSrv.fixSyntaxForScala3(unmanagedSources)) {
                        case (finiteDuration, Success(_)) =>
                          scribe.info(s"Successfully run fixSyntaxForScala3  in $finiteDuration")
                        case (_, Failure(e)) =>
                          scribe.info(s"""|Failed running scalafix to fix syntax for scala 3
                                          |Cause: ${e.getMessage}""".stripMargin)
                      }
    } yield scalafixEval
  }

  def prepareMigration(unmanagedSources: Seq[AbsolutePath]): Try[Unit] =
    for {
      scalafixEval <- previewPrepareMigration(unmanagedSources)
      _             = scalafixSrv.fixInPlace(scalafixEval)
    } yield ()

  private def setupScala3Compiler(
    classpath: Classpath,
    classDirectory: AbsolutePath,
    scala3CompilerOptions: Seq[String]
  ): Try[Scala3Compiler] = {
    // It's easier no to deal with semanticdb option, since we don't need semanticdb files
    val modified           = scala3CompilerOptions.filterNot(_ == "-Xsemanticdb")
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
      _ <- timeAndLog(Try(compiler.compileAndReport(cuManagedSources.toList ++ cuUnmanagedSources.toList, reporter))) {
             case (finiteDuration, Success(_)) =>
               scribe.info(s"Successfully compiled with scala 3 in $finiteDuration")
             case (_, Failure(e)) =>
               scribe.info(s"""|Compilation with scala 3 failed.
                               |Please fix the errors above.""".stripMargin)
           }

    } yield ()

  private def buildMigrationFiles(unmanagedSources: Seq[AbsolutePath]): Try[Seq[FileMigrationState.Initial]] =
    for {
      fileEvaluations <-
        timeAndLog(scalafixSrv.inferTypesAndImplicits(unmanagedSources)) {
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
object ScalaMigrat {
  def migrateScalacOptions(scalacOptions: Seq[String]): MigratedScalacOptions = {
    val sanitized                              = ScalacOption.sanitizeScalacOption(scalacOptions)
    val scalaSettings                          = sanitized.map(ScalacOption.from)
    val notParsed: Seq[ScalacOption.NotParsed] = scalaSettings.collect { case x: ScalacOption.NotParsed => x }
    val scala3cOption: Seq[Scala3cOption] = scalaSettings.collect {
      case x: ScalacOption.Specific3 => x;
      case x: ScalacOption.Shared    => x
    }
    val renamed   = scalaSettings.collect { case x: ScalacOption.Renamed => x }
    val specific2 = scalaSettings.collect { case x: ScalacOption.Specific2 => x }
    MigratedScalacOptions(notParsed, specific2, scala3cOption ++ renamed)
  }

  def migrateLibs(libs: Seq[Lib]): Map[Lib213, Seq[CompatibleWithScala3Lib]] = {
    val libsCompatibleWith213 = libs.map(l => l -> Lib213.from(l)).toMap
    libsCompatibleWith213.collect { case (lib, None) =>
      scribe.info(s"Not able to parse the crossVersion of ${lib}: ${lib.getCrossVersion}")
    }
    migrate213Libs(libsCompatibleWith213.values.flatten.toSeq)
  }

  private def migrate213Libs(libs: Seq[Lib213]): Map[Lib213, Seq[CompatibleWithScala3Lib]] =
    libs.map(lib => (lib, lib.toCompatible)).toMap

}
