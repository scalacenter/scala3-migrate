package migrate

import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import compiler.interfaces.Scala3Compiler
import migrate.internal._
import migrate.utils.FileUtils
import migrate.utils.ScalaExtensions._
import migrate.utils.Timer._
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixEvaluation

object Main {
  lazy val scalafix: Scalafix               = Scalafix.fetchAndClassloadInstance("2.13")
  lazy val scalafixClassLoader: ClassLoader = scalafix.getClass().getClassLoader()

  def previewMigration(
    sources: Seq[AbsolutePath],
    scala2Classpath: Classpath,
    scala2CompilerOptions: Seq[String],
    toolClasspath: Classpath,
    targetRoot: AbsolutePath,
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Map[AbsolutePath, FileMigrationState.FinalState]] = {
    scribe.info(s"""Migrating
                   |${sources.toList.mkString("\n")}""".stripMargin)
    for {
      compiler <- setupScala3Compiler(scala3Classpath, scala3ClassDirectory, scala3CompilerOptions)
      initialFileToMigrate <-
        buildMigrationFiles(sources, scala2Classpath, targetRoot, toolClasspath, scala2CompilerOptions)
      _            <- compileInScala3(initialFileToMigrate, compiler)
      migratedFiles = initialFileToMigrate.map(f => (f.source, f.migrate(compiler))).toMap
    } yield migratedFiles
  }

  def migrate(
    sources: Seq[AbsolutePath],
    scala2Classpath: Classpath,
    scala2CompilerOptions: Seq[String],
    toolClasspath: Classpath,
    targetRoot: AbsolutePath,
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[Unit] =
    for {
      migratedFiles <- previewMigration(
                         sources,
                         scala2Classpath,
                         scala2CompilerOptions,
                         toolClasspath,
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

  private def compileInScala3(migrationFiles: Seq[FileMigrationState], compiler: Scala3Compiler): Try[Unit] =
    for {
      compilationUnits <- migrationFiles.map(_.previewAllPatches()).sequence
      _ <- timedMs {
             Try {
               compiler.compile(compilationUnits.toList)
             }
           } match {
             case (success @ Success(_), finiteduration) =>
               scribe.info(s"Compiled ${migrationFiles.size} file(s) successfully after ${finiteduration}")
               success
             case (failure @ Failure(cause), _) =>
               scribe.error(s"Compilation failed", cause)
               failure
           }
    } yield ()

  private def buildMigrationFiles(
    sources: Seq[AbsolutePath],
    classpath: Classpath,
    targetRoot: AbsolutePath,
    toolClasspath: Classpath,
    compilerOptions: Seq[String]
  ): Try[Seq[FileMigrationState.Initial]] =
    for {
      fileEvaluations <- timedMs {
                           inferTypes(sources, classpath, toolClasspath, compilerOptions, targetRoot)
                         } match {
                           case (Success(evaluation), finiteDuration) =>
                             val fileEvaluations = evaluation.getFileEvaluations().toSeq
                             val patchesCount    = fileEvaluations.map(_.getPatches().size).sum
                             scribe.info(
                               s"Found $patchesCount patch candidate(s) in ${sources.size} file(s) after $finiteDuration"
                             )
                             Success(fileEvaluations)

                           case (Failure(cause), _) =>
                             scribe.error("Failed inferring types", cause)
                             Failure(cause)
                         }
      fileEvaluationMap <- fileEvaluations
                             .map(e => AbsolutePath.from(e.getEvaluatedFile()).map(file => file -> e))
                             .sequence
                             .map(_.toMap)
      fileToMigrate <- sources.map(src => fileEvaluationMap.get(src).map(FileMigrationState.Initial).toTry).sequence
    } yield fileToMigrate

  private def inferTypes(
    sources: Seq[AbsolutePath],
    classpath: Classpath,
    toolClasspath: Classpath,
    compilerOptions: Seq[String],
    targetRootSemantic: AbsolutePath
  ): Try[ScalafixEvaluation] = Try {
    val classpathWithTargetSemantic = classpath :+ targetRootSemantic
    val args = scalafix
      .newArguments()
      .withRules(Seq("MigrationRule", "ExplicitImplicits").asJava)
      .withPaths(sources.map(_.toNio).asJava)
      .withClasspath(classpathWithTargetSemantic.paths.map(_.toNio).asJava)
      .withScalacOptions(compilerOptions.asJava)
      .withToolClasspath(toolClasspath.toUrlClassLoader(scalafixClassLoader))
    args.evaluate()
  }
}
