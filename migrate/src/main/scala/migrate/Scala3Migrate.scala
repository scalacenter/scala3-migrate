package migrate

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import compiler.interfaces.CompilationUnit
import compiler.interfaces.Scala3Compiler
import migrate.interfaces.Lib
import migrate.interfaces.MigratedLibsImpl
import migrate.interfaces.MigratedScalacOptions
import migrate.internal.ScalaMigrateLogger
import migrate.internal.ScalacOption.PluginSpecific
import migrate.internal._
import migrate.utils.FileUtils
import migrate.utils.ScalaExtensions._
import migrate.utils.ScalafixService
import migrate.utils.Timer._
import scalafix.interfaces.ScalafixEvaluation

class Scala3Migrate(scalafixSrv: ScalafixService) {
  private val reporter = ScalaMigrateLogger

  def previewMigration(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Try[Map[AbsolutePath, FileMigrationState.FinalState]] = {
    unmanagedSources.foreach(f => scribe.info(s"Migrating $f"))
    val (scalaFiles, javaFiles) = unmanagedSources.partition(_.value.endsWith("scala"))
    // first try to compile without adding any patch
    val filesWithErr = compileInScala3AndGetFilesWithErrors(unmanagedSources ++ managedSources, compiler)
    if (filesWithErr.isEmpty) {
      scribe.info("The project compiles successfully in Scala 3")
      Success(Map[AbsolutePath, FileMigrationState.FinalState]())
    } else {
      val filesWithoutErrors = scalaFiles.diff(filesWithErr)
      (for {
        initialFileToMigrate <- buildMigrationFiles(filesWithErr)
        _                    <- compileInScala3(initialFileToMigrate, filesWithoutErrors ++ javaFiles ++ managedSources, compiler)
        migratedFiles        <- initialFileToMigrate.map(f => f.migrate(compiler).map(success => (f.source, success))).sequence
      } yield migratedFiles.toMap)
    }
  }

  def migrate(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Unit] =
    for {
      compiler <- setupScala3Compiler(scala3Classpath, scala3ClassDirectory, scala3CompilerOptions)
      migratedFiles <-
        previewMigration(unmanagedSources, managedSources, compiler)
      _ <- migratedFiles.map { case (file, migrated) =>
             migrated.newFileContent.flatMap(FileUtils.writeFile(file, _))
           }.sequence
      _ = migratedFiles.keys.map(file => scribe.info(s"${file.value} has been successfully migrated"))
      _ <- compileWithRewrite(
             scala3Classpath,
             scala3ClassDirectory,
             scala3CompilerOptions,
             unmanagedSources,
             managedSources
           )
    } yield ()

  def previewSyntaxMigration(unmanagedSources: Seq[AbsolutePath]): Try[ScalafixEvaluation] = {
    unmanagedSources.foreach(f => scribe.debug(s"Fixing syntax of $f"))
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

  def migrateSyntax(unmanagedSources: Seq[AbsolutePath]): Try[Unit] =
    for {
      scalafixEval <- previewSyntaxMigration(unmanagedSources)
      _             = scalafixSrv.fixInPlace(scalafixEval)
    } yield ()

  private[migrate] def setupScala3Compiler(
    classpath: Classpath,
    classDirectory: AbsolutePath,
    scala3CompilerOptions: Seq[String]
  ): Try[Scala3Compiler] = {
    // It's easier no to deal with semanticdb option, since we don't need semanticdb files
    val modified           = scala3CompilerOptions.filterNot(elm => elm == "-Xsemanticdb" || elm == "-Ysemanticdb")
    val scala3CompilerArgs = modified.toArray ++ Array("-classpath", classpath.value, "-d", classDirectory.value)
    Try {
      Scala3Compiler.setup(scala3CompilerArgs)
    }
  }

  private def compileWithRewrite(
    classpath3: Classpath,
    classDir3: AbsolutePath,
    settings3: Seq[String],
    unmanaged: Seq[AbsolutePath],
    managed: Seq[AbsolutePath]
  ): Try[Unit] = {
    scribe.info(s"Finalizing the migration: compiling in scala 3 with -rewrite option")
    for {
      compilerWithRewrite <- setupScala3Compiler(classpath3, classDir3, settings3 :+ "-rewrite")
      _                   <- Try(compilerWithRewrite.compileWithRewrite((unmanaged ++ managed).map(_.value).toList))
    } yield ()
  }

  private def compileInScala3(
    migrationFiles: Seq[FileMigrationState],
    managedSources: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Try[Unit] =
    for {
      cuUnmanagedSources <- migrationFiles.map(_.previewAllPatches()).sequence
      cuManagedSources    = managedSources.map(path => new CompilationUnit(path.value, FileUtils.read(path)))
      _ <- timeAndLog(Try(compiler.compileAndReport((cuUnmanagedSources ++ cuManagedSources).toList, reporter))) {
             case (finiteDuration, Success(_)) =>
               scribe.info(s"Successfully compiled with scala 3 in $finiteDuration")
             case (_, Failure(_)) =>
               scribe.info(s"""|Compilation with scala 3 failed.
                               |Please fix the errors above.""".stripMargin)
           }
    } yield ()

  private def compileInScala3AndGetFilesWithErrors(
    files: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Seq[AbsolutePath] = {
    val compilationUnits = files.map(path => new CompilationUnit(path.value, FileUtils.read(path)))
    val res              = compiler.compileAndReportFilesWithErrors(compilationUnits.toList).toSeq
    res.map(AbsolutePath.from(_)).sequence.getOrElse(Nil)
  }

  private def buildMigrationFiles(unmanagedSources: Seq[AbsolutePath]): Try[Seq[FileMigrationState.Initial]] =
    if (unmanagedSources.isEmpty) Success(Seq())
    else
      for {
        fileEvaluations <-
          timeAndLog(scalafixSrv.inferTypesAndImplicits(unmanagedSources)) {
            case (duration, Success(files)) =>
              val fileEvaluationsSeq = files.getFileEvaluations().toSeq
              val patchesCount       = fileEvaluationsSeq.map(_.getPatches().size).sum
              scribe.info(
                s"Found ${patchesCount} patch candidate(s) in ${unmanagedSources.size} file(s)after $duration"
              )
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

object Scala3Migrate {
  def migrateScalacOptions(scalacOptions: Seq[String]): MigratedScalacOptions = {
    val sanitized                              = ScalacOption.sanitizeScalacOption(scalacOptions)
    val scalaSettings                          = sanitized.map(ScalacOption.from)
    val notParsed: Seq[ScalacOption.NotParsed] = scalaSettings.collect { case x: ScalacOption.NotParsed => x }
    val scala3cOption: Seq[Scala3cOption] = scalaSettings.collect {
      case x: ScalacOption.Specific3 => x;
      case x: ScalacOption.Shared    => x
    }
    val pluginsSettings: Seq[PluginSpecific] = scalaSettings.collect { case x: ScalacOption.PluginSpecific => x }
    val renamed                              = scalaSettings.collect { case x: ScalacOption.Renamed => x }
    val specific2                            = scalaSettings.collect { case x: ScalacOption.Specific2 => x }
    MigratedScalacOptions(notParsed, specific2, scala3cOption, renamed, pluginsSettings)
  }

  def migrateLibs(libs: Seq[Lib]): MigratedLibsImpl = {
    val libsCompatibleWith213 = libs.map(l => l -> InitialLib.from(l)).toMap
    libsCompatibleWith213.collect { case (lib, None) =>
      scribe.info(s"Not able to parse the crossVersion of ${lib}: ${lib.getCrossVersion}")
    }
    val allParsedLibs = libsCompatibleWith213.values.flatten.toSeq
    val filteredLibs = allParsedLibs.filterNot(l =>
      InitialLib.filteredLibs.exists { case (org, name) => org == l.organization && name == l.name })
    MigratedLibsImpl.from(filteredLibs.map(lib => (lib, lib.toCompatible)).toMap)
  }

}
