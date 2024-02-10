package migrate

import migrate.interfaces.CompilationException
import ScalaMigratePlugin.{Keys, inputsStore}
import sbt.Keys._
import sbt._

import scala.io.AnsiColor._
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import java.nio.file.Files

private[migrate] object TypeInferenceMigration {
  val internalImpl = Def.taskDyn {
    val projectRef = thisProjectRef.value
    val configs    = Keys.migrationConfigs.value
    val sv         = scalaVersion.value
    val projectId  = projectRef.project

    if (sv != BuildInfo.scala3Version)
      sys.error(s"Expecting scalaVersion to be ${BuildInfo.scala3Version}")

    Def.sequential(configs.map(migrateConfig(projectRef, projectId)(_)) :+ success(projectId))
  }

  private def migrateConfig(projectRef: ProjectRef, projectId: String)(
    config: Configuration
  ): Def.Initialize[Task[Unit]] =
    Def.task {
      val logger       = streams.value.log
      val scala3Inputs = (config / Keys.scala3Inputs).value
      val scope        = (projectRef / config / Keys.scala2Inputs).scope
      val scala2Inputs = inputsStore.getOrElse(scope, sys.error("no input found"))
      val baseDir      = baseDirectory.value

      logger.info(startingMessage(projectId, config.id))

      if (scala2Inputs.unmanagedSources.nonEmpty) {
        if (!Files.exists(scala3Inputs.classDirectory)) Files.createDirectory(scala3Inputs.classDirectory)
        Try {
          val migrateAPI = ScalaMigratePlugin.getMigrateInstance(logger)
          migrateAPI.migrate(
            scala2Inputs.unmanagedSources.asJava,
            scala2Inputs.managedSources.asJava,
            scala3Inputs.semanticdbTarget,
            scala2Inputs.classpath.asJava,
            scala2Inputs.scalacOptions.asJava,
            scala3Inputs.classpath.asJava,
            scala3Inputs.scalacOptions.asJava,
            scala3Inputs.classDirectory,
            baseDir.toPath
          )
        } match {
          case Success(_) =>
          case Failure(_: CompilationException) =>
            val message =
              s"""|Migration of $projectId / $config failed because of a compilation error.
                  |Fix the error and try again.
                  |""".stripMargin
            throw new MessageOnlyException(message)
          case Failure(cause) =>
            logger.error(s"Migration of $projectId / $config failed.")
            throw cause
        }
      } else logger.debug(s"There is no unmanagedSources to migrate for $config")
    }

  private def startingMessage(projectId: String, config: String): String =
    s"""|
        |${BOLD}Migrating types in $projectId / $config$RESET
        |
        |""".stripMargin

  private def success(projectId: String) = Def.task {
    val logger = streams.value.log
    val message =
      s"""|
          |You can safely upgrade $projectId to Scala 3:
          |${YELLOW}scalaVersion := "${BuildInfo.scala3Version}"$RESET
          |""".stripMargin
    logger.info(message)
  }
}
