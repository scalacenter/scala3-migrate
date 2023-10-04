package migrate

import sbt._
import sbt.internal.util.ManagedLogger

import ScalaMigratePlugin.Keys._
import Messages._

import scala.collection.JavaConverters._
import scala.Console._

private[migrate] object ScalacOptionsMigration {

  lazy val internalImpl: Def.Initialize[Task[Unit]] = Def.task {
    val logger           = Keys.streams.value.log
    val projectId        = Keys.thisProject.value.id
    val allScalacOptions = allScalacOptionsTask.value
    val sv               = Keys.scalaVersion.value

    if (!sv.startsWith("2.13.") && !sv.startsWith("3."))
      throw new MessageOnlyException(Messages.notScala213(sv, projectId))

    logger.info(startingMessage(projectId))

    val migrateAPI = ScalaMigratePlugin.getMigrateInstance(logger)
    val migrated   = migrateAPI.migrateScalacOption(allScalacOptions.asJava)

    if (migrated.getValid.nonEmpty) {
      logger.info(validMessage(migrated.getValid))
    }

    val renamed = migrated.getRenamed.asScala.toMap
    if (renamed.nonEmpty) {
      logger.warn(renamedMessage(renamed))
    }

    if (migrated.getRemoved.nonEmpty) {
      logger.warn(removedMessage(migrated.getRemoved))
    }

    if (migrated.getUnknown.nonEmpty) {
      logger.warn(unknownMessage(migrated.getUnknown))
    }
  }

  private val allScalacOptionsTask: Def.Initialize[Task[Seq[String]]] = Def.taskDyn {
    val configs = migrationConfigs.value
    Def.task {
      configs
        .map(c => (c / Keys.scalacOptions).result)
        .join(_.join)
        .value
        .collect { case Value(scalacOptions) => scalacOptions }
        .flatten
    }
  }

  private def startingMessage(projectId: String): String =
    s"""|
        |${BOLD}Starting migration of scalacOptions in $projectId${RESET}
        |""".stripMargin

  private def validMessage(scalacOptions: Seq[String]): String =
    s"""|
        |${GREEN}${BOLD}Valid scalacOptions:$RESET
        |${scalacOptions.mkString("\n")}
        |""".stripMargin

  private def renamedMessage(scalacOptions: Map[String, String]): String = {
    val maxSize = scalacOptions.keys.map(_.size).max
    def format(keyValue: (String, String)): String = {
      val (key, value) = keyValue
      val spaces       = " " * (maxSize - key.size)
      s"$key$spaces -> $YELLOW$value$RESET"
    }
    s"""|
        |${YELLOW}${BOLD}Renamed scalacOptions:${RESET}
        |${scalacOptions.map(format).mkString("\n")}
        |""".stripMargin
  }

  private def removedMessage(scalacOptions: Seq[String]): String =
    s"""|
        |$YELLOW${BOLD}Removed scalacOptions:$RESET
        |${scalacOptions.mkString("\n")}
        |""".stripMargin

  private def unknownMessage(scalacOptions: Seq[String]): String =
    s"""|
        |$YELLOW${BOLD}Unknonw scalacOptions:$RESET
        |${scalacOptions.mkString("\n")}
        |""".stripMargin
}
