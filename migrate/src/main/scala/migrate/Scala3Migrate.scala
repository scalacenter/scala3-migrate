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

class Scala3Migrate(scalafixSrv: ScalafixService, baseDirectory: AbsolutePath, logger: Logger) {

  def previewMigration(
    unmanagedSources: Seq[AbsolutePath],
    managedSources: Seq[AbsolutePath],
    compiler: Scala3Compiler
  ): Try[Map[AbsolutePath, FileMigrationState.FinalState]] = {
    val (scalaFiles, javaFiles) = unmanagedSources.partition(_.value.endsWith("scala"))
    // first try to compile without adding any patch
    val filesWithErr = compileInScala3AndGetFilesWithErrors(unmanagedSources ++ managedSources, compiler)
    if (filesWithErr.isEmpty) { Success(Map[AbsolutePath, FileMigrationState.FinalState]()) }
    else {
      val filesWithoutErrors = scalaFiles.diff(filesWithErr)
      for {
        initialFileToMigrate <- buildMigrationFiles(filesWithErr)
        _                    <- compileInScala3(initialFileToMigrate, filesWithoutErrors ++ javaFiles ++ managedSources, compiler)
        migratedFiles <- initialFileToMigrate
                           .map(f => f.migrate(compiler, logger).map(success => (f.source, success)))
                           .sequence
      } yield migratedFiles.toMap
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
      compiler      <- setupScala3Compiler(scala3Classpath, scala3ClassDirectory, scala3CompilerOptions)
      migratedFiles <- previewMigration(unmanagedSources, managedSources, compiler)
      _ <- migratedFiles.map { case (file, migrated) =>
             migrated.newFileContent.flatMap(FileUtils.writeFile(file, _))
           }.sequence
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
                        case (_, Success(_)) =>
                          logger.info(
                            s"Run syntactic rules in ${plural(unmanagedSources.size, "Scala source")} successfully")
                        case (_, Failure(e)) =>
                          logger.error(s"Failed running syntactic rules because: ${e.getMessage}")
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
    Try(Scala3Compiler.setup(scala3CompilerArgs))
  }

  private def compileWithRewrite(
    classpath3: Classpath,
    classDir3: AbsolutePath,
    settings3: Seq[String],
    unmanaged: Seq[AbsolutePath],
    managed: Seq[AbsolutePath]
  ): Try[Unit] = {
    logger.info(s"Compiling to Scala 3 with -source:3.0-migration -rewrite")
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
             case (_, Failure(_)) =>
               logger.error("Failed inferring meaningful types because: Scala 3 compilation error")
             case _ =>
               val count = migrationFiles.map(_.patches.size).sum
               val message =
                 s"Found ${plural(count, "patch", "patches")} in ${plural(migrationFiles.size, "Scala source")}"
               logger.info(message)
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
        fileEvaluations <- timeAndLog(scalafixSrv.inferTypesAndImplicits(unmanagedSources)) {
                             case (_, Failure(e)) =>
                               logger.error(s"Failed inferring types because: ${e.getMessage}")
                             case _ =>
                           }
        fileEvaluationMap <- fileEvaluations.getFileEvaluations.toSeq
                               .map(e => AbsolutePath.from(e.getEvaluatedFile).map(file => file -> e))
                               .sequence
                               .map(_.toMap)
        fileToMigrate <-
          unmanagedSources
            .map(src => fileEvaluationMap.get(src).map(FileMigrationState.Initial(_, baseDirectory)).toTry)
            .sequence
      } yield fileToMigrate
}
