package migrate

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import migrate.compiler.interfaces.CompilationUnit
import migrate.compiler.interfaces.Scala3Compiler
import migrate.interfaces.Logger
import migrate.internal._
import migrate.utils.FileUtils
import migrate.utils.Format._
import migrate.utils.ScalaExtensions._
import migrate.utils.ScalafixService
import migrate.utils.Timer._
import scalafix.interfaces.ScalafixEvaluation

class Scala3Migrate(scalafixSrv: ScalafixService, logger: Logger) {

  def previewMigration(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Try[Map[AbsolutePath, FileMigrationState.FinalState]] = {
    unmanagedSources.foreach(f => logger.info(s"Migrating $f"))
    val (scalaFiles, javaFiles) = unmanagedSources.partition(_.value.endsWith("scala"))
    // first try to compile without adding any patch
    val filesWithErr = compileInScala3AndGetFilesWithErrors(unmanagedSources ++ managedSources, compiler)
    if (filesWithErr.isEmpty) {
      logger.info("The project compiles successfully in Scala 3")
      Success(Map[AbsolutePath, FileMigrationState.FinalState]())
    } else {
      val filesWithoutErrors = scalaFiles.diff(filesWithErr)
      (for {
        initialFileToMigrate <- buildMigrationFiles(filesWithErr)
        _                    <- compileInScala3(initialFileToMigrate, filesWithoutErrors ++ javaFiles ++ managedSources, compiler)
        migratedFiles <-
          initialFileToMigrate.map(f => f.migrate(compiler, logger).map(success => (f.source, success))).sequence
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
      _ = migratedFiles.keys.map(file => logger.info(s"${file.value} has been successfully migrated"))
      _ <- compileWithRewrite(
             scala3Classpath,
             scala3ClassDirectory,
             scala3CompilerOptions,
             unmanagedSources,
             managedSources
           )
    } yield ()

  def previewSyntaxMigration(unmanagedSources: Seq[AbsolutePath]): Try[ScalafixEvaluation] =
    for {
      scalafixEval <- timeAndLog(scalafixSrv.fixSyntaxForScala3(unmanagedSources)) {
                        case (finiteDuration, Success(_)) =>
                          logger.info(
                            s"Run syntactic rules in ${plural(unmanagedSources.size, "file")} successfully after $finiteDuration")
                        case (_, Failure(e)) =>
                          logger.info(s"Failed running syntactic rules because: ${e.getMessage}")
                      }
    } yield scalafixEval

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
    logger.info(s"Compiling in scala 3 with -rewrite option")
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
      cuManagedSources    = managedSources.map(path => new CompilationUnit(path.value, FileUtils.read(path), path.toNio))
      _ <- timeAndLog(Try(compiler.compileAndReport((cuUnmanagedSources ++ cuManagedSources).toList, logger))) {
             case (finiteDuration, Success(_)) =>
               logger.info(s"Scala 3 compilation succeeded after $finiteDuration")
             case (_, Failure(_)) =>
               logger.error("Scala 3 compilation failed. Try to fix the error(s) manually")
           }
    } yield ()

  private def compileInScala3AndGetFilesWithErrors(
    files: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Seq[AbsolutePath] = {
    val compilationUnits = files.map(path => new CompilationUnit(path.value, FileUtils.read(path), path.toNio))
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
              val count              = fileEvaluationsSeq.map(_.getPatches().size).sum
              logger.info(
                s"Found ${plural(count, "patch", "patches")} in ${plural(unmanagedSources.size, "file")} after $duration")
            case (_, Failure(e)) =>
              logger.error(s"Failed inferring types because: ${e.getMessage()}.")
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
