package migrate

import migrate.ScalaMigratePlugin.Keys._
import migrate.ScalaMigratePlugin.scala3Version
import ScalaMigratePlugin.migrateAPI

import sbt.Keys._
import sbt._

import scala.io.AnsiColor._
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConverters._

private[migrate] object SyntaxMigration {
  val internalImpl = Def.taskDyn {
    val configs   = migrationConfigs.value
    val projectId = thisProject.value.id
    val sv        = scalaVersion.value

    if (!sv.startsWith("2.13."))
      throw new MessageOnlyException(Messages.notScala213(sv, projectId))

    Def.task {
      val logger          = streams.value.log
      val _               = configs.map(_ / compile).join.value
      val allScala2Inputs = configs.map(_ / scala2Inputs).join.value

      logger.info(welcome(projectId, configs.map(_.id)))

      for {
        (scala2Inputs, config) <- allScala2Inputs.zip(configs)
        if scala2Inputs.unmanagedSources.nonEmpty
      } Try {
        logger.info(scala2Inputs.unmanagedSources.mkString("\n"))
        migrateAPI.migrateSyntax(
          scala2Inputs.unmanagedSources.asJava,
          scala2Inputs.semanticdbTarget,
          scala2Inputs.classpath.asJava,
          scala2Inputs.scalacOptions.asJava
        )
      } match {
        case scala.util.Success(_) =>
          logger.info(success(projectId, config.id))
        case Failure(exception) =>
          logger.err(error(projectId, config.id, exception))
      }

      logger.info(nextCommand(projectId))
    }
  }

  private def welcome(projectId: String, configs: Seq[String]): String =
    s"""|
        |${BOLD}We are going to fix some syntax incompatibilities in $projectId / ${configs
      .mkString("[", ", ", "]")}${RESET}
        |
        |""".stripMargin

  private def success(projectId: String, config: String) =
    s"""|
        |The syntax incompatibilities have been fixed in $projectId / $config
        |
        |""".stripMargin

  private def error(projectId: String, config: String, ex: Throwable): String =
    s"""|
        |Failed fixing the syntax in $projectId / $config
        |${ex.getMessage()}
        |${ex.getStackTrace.mkString("\n")}
        |
        |""".stripMargin

  private def nextCommand(projectId: String): String =
    s"""|
        |You can now commit the change!
        |Then you can run the next command:
        |
        |migrateTypes $projectId
        |""".stripMargin
}
