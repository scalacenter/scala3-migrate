package migrate

import java.nio.file.Files
import interfaces._
import scalafix.interfaces.{Scalafix, ScalafixEvaluation}
import utils.ScalaExtensions._
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import internal._
import utils.Timer._

object Main  {
  lazy val scalafix = Scalafix.fetchAndClassloadInstance("2.13")
  lazy val scalafixClassLoader = scalafix.getClass().getClassLoader()

  def previewMigration(
    sourceRoot: AbsolutePath,
    source: AbsolutePath,
    scala2Classpath: Classpath,
    scala2CompilerOptions: Seq[String],
    toolClasspath: Classpath,
    scala3Classpath: Classpath,
    scala3CompilerOptions: Seq[String],
    scala3ClassDirectory: AbsolutePath
  ): Try[String] = {
    for {
      compiler <- setupScala3Compiler(scala3Classpath, scala3ClassDirectory)
      migrationFiles <- buildMigrationFiles(
        sourceRoot,
        Seq(source), 
        scala2Classpath,
        toolClasspath,
        scala2CompilerOptions
      )
      _ <- compileInScala3(migrationFiles, compiler)
      migrationFile = migrationFiles.head
      result <- migrate(migrationFile, compiler) match {
        case FileMigrationFailure(cause) => Failure(cause)
        case FileMigrationSuccess(patches) => migrationFile.previewPatches(patches)
      }
    } yield result.content
  }

  private def setupScala3Compiler(classpath: Classpath, classDirectory: AbsolutePath): Try[Scala3Compiler] = {
    val scala3CompilerArgs = Array(
      "-classpath", classpath.value,
      "-d", classDirectory.value
    )
    Try { Scala3Compiler.setup(scala3CompilerArgs) }
  }

  private def compileInScala3(
    migrationFiles: List[MigrationFile],
    compiler: Scala3Compiler
  ): Try[Unit] = {
    for {
      compilationUnits <- migrationFiles.map(_.previewAllPatches()).sequence
      _ <- timedMs { 
        Try { compiler.compile(compilationUnits) }
      } match {
        case (success @ Success(_), timeMs) =>
          scribe.info(s"Compiled ${migrationFiles.size} file(s) successfully after $timeMs ms")
          success
        case (failure @ Failure(cause), _) =>
          scribe.error(s"Compilation failed", cause)
          failure
      }
    } yield ()
  }

  private def migrate(file: MigrationFile, compiler: Scala3Compiler): FileMigrationResult = {
    new FileMigration(file, compiler).migrate()
  }

  private def buildMigrationFiles(
    sourceRoot: AbsolutePath,
    sources: Seq[AbsolutePath],
    classpath: Classpath,
    toolClasspath: Classpath,
    compilerOptions: Seq[String]
  ): Try[List[MigrationFile]] = {
    for {
      fileEvaluations <- timedMs {
        inferTypes(sourceRoot, sources, classpath, toolClasspath, compilerOptions)
      } match {
        case (Success(evaluation), timeMs) => 
          val fileEvaluations = evaluation.getFileEvaluations().toSeq
          val patchesCount = fileEvaluations.map(_.getPatches().size).sum
          scribe.info(s"Found $patchesCount patch candidate(s) in ${sources.size} file(s) after $timeMs ms")
          Success(fileEvaluations)
        
        case (failure @ Failure(cause), _) =>
          scribe.error("Failed inferring types", cause)
          Failure(cause)
      }
      fileEvaluationMap <- fileEvaluations
        .map(e => AbsolutePath.from(e.getEvaluatedFile()).map(file => file -> e))
        .sequence.map(_.toMap)
    } yield {
      sources
        .map(src =>MigrationFile(src, fileEvaluationMap.get(src)))
        .toList
    }
  }

  private def inferTypes(
    sourceRoot: AbsolutePath,
    sources: Seq[AbsolutePath],
    classpath: Classpath,
    toolClasspath: Classpath,
    compilerOptions: Seq[String]
  ): Try[ScalafixEvaluation] = Try {
    val args = scalafix.newArguments()
      .withRules(Seq("Infertypes").asJava)
      .withPaths(sources.map(_.toNio).asJava)
      .withClasspath(classpath.paths.map(_.toNio).asJava)
      .withScalacOptions(compilerOptions.asJava)
      .withToolClasspath(toolClasspath.toUrlClassLoader(scalafixClassLoader))
      .withSourceroot(sourceRoot.toNio)
    args.evaluate()
  }
}
