package migrate

import interfaces._
import migrate.internal._
import migrate.utils.ScalaExtensions._
import migrate.utils.Timer._
import scalafix.interfaces.{Scalafix, ScalafixEvaluation}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object Main {
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
      initialFileToMigrate <- buildMigrationFiles(
        sourceRoot,
        Seq(source),
        scala2Classpath,
        toolClasspath,
        scala2CompilerOptions
      )
      _ <- compileInScala3(initialFileToMigrate, compiler)
      initialFile = initialFileToMigrate.head
      result <- initialFile.migrate(compiler) match {
        case FileMigrationState.Failed(_, cause) => Failure(cause)
        case f@FileMigrationState.Succeeded(_, patches) => f.previewPatches(patches)
      }
    } yield result.content
  }

  private def setupScala3Compiler(classpath: Classpath, classDirectory: AbsolutePath): Try[Scala3Compiler] = {
    val scala3CompilerArgs = Array(
      "-classpath", classpath.value,
      "-d", classDirectory.value
    )
    Try {
      Scala3Compiler.setup(scala3CompilerArgs)
    }
  }

  private def compileInScala3(migrationFiles: Seq[FileMigrationState],
                              compiler: Scala3Compiler
                             ): Try[Unit] = {
    for {
      compilationUnits <- migrationFiles.map(_.previewAllPatches()).sequence
      _ <- timedMs {
        Try {
          compiler.compile(compilationUnits.toList)
        }
      } match {
        case (success@Success(_), finiteduration) =>
          scribe.info(s"Compiled ${migrationFiles.size} file(s) successfully after ${finiteduration}")
          success
        case (failure@Failure(cause), _) =>
          scribe.error(s"Compilation failed", cause)
          failure
      }
    } yield ()
  }


  private def buildMigrationFiles(sourceRoot: AbsolutePath,
                                  sources: Seq[AbsolutePath],
                                  classpath: Classpath,
                                  toolClasspath: Classpath,
                                  compilerOptions: Seq[String]
                                 ): Try[Seq[FileMigrationState.Initial]] = {
    for {
      fileEvaluations <- timedMs {
        inferTypes(sourceRoot, sources, classpath, toolClasspath, compilerOptions)
      } match {
        case (Success(evaluation), finiteDuration) =>
          val fileEvaluations = evaluation.getFileEvaluations().toSeq
          val patchesCount = fileEvaluations.map(_.getPatches().size).sum
          scribe.info(s"Found $patchesCount patch candidate(s) in ${sources.size} file(s) after $finiteDuration")
          Success(fileEvaluations)

        case (failure@Failure(cause), _) =>
          scribe.error("Failed inferring types", cause)
          Failure(cause)
      }
      fileEvaluationMap <- fileEvaluations
        .map(e => AbsolutePath.from(e.getEvaluatedFile()).map(file => file -> e))
        .sequence.map(_.toMap)
      fileToMigrate <- sources.map(src => fileEvaluationMap.get(src).map(FileMigrationState.Initial).toTry).sequence
    } yield fileToMigrate
  }

  private def inferTypes(sourceRoot: AbsolutePath,
                         sources: Seq[AbsolutePath],
                         classpath: Classpath,
                         toolClasspath: Classpath,
                         compilerOptions: Seq[String]
                        ): Try[ScalafixEvaluation] = Try {
    val args = scalafix.newArguments()
      .withRules(Seq("Infertypes").asJava)
      .withPaths(sources.map(_.toNio).asJava)
      .withClasspath(classpath.paths.map(_.toNio).asJava)
      .withScalacOptions(compilerOptions.asJava) // not sure which compiler option we need here !!
      .withToolClasspath(toolClasspath.toUrlClassLoader(scalafixClassLoader))
      .withSourceroot(sourceRoot.toNio)
    args.evaluate()
  }
}
