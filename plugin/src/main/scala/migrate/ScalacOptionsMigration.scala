package migrate

import sbt.Def
import sbt.Keys._
import sbt.ScopeFilter
import sbt._
import sbt.internal.util.ManagedLogger

import migrate.interfaces.ScalacOptions
import ScalaMigratePlugin.Keys._
import Messages._

import scala.collection.JavaConverters._
import scala.io.AnsiColor._

object ScalacOptionsMigration {
  private[migrate] val internalImpl = Def.taskDyn {
    val logger  = streams.value.log
    val configs = migrationConfigs.value

    val projectRef = thisProject.value
    logger.info(starting(projectRef.id, configs.map(_.id)))
    logger.info(warning)
    logger.info(help)

    val commonOptions = scalacOptions.value
    val filter        = ScopeFilter.apply(configurations = inConfigurations(configs: _*))

    if (commonOptions.nonEmpty) {
      val migrationStatus = ScalaMigratePlugin.migrateAPI.migrateScalacOption(commonOptions.asJava)
      reportStatus(logger, migrationStatus)
    }

    Def.task {
      val configOptions = scalacOptions.all(filter).value
      for {
        (options, config) <- configOptions.zip(configs)
        filteredOptions    = options.filterNot(opt => commonOptions.contains(opt))
        if filteredOptions.nonEmpty
      } {
        logger.info(s"${BOLD}In configuration ${config.id}:${RESET}")
        val migrationStatus = ScalaMigratePlugin.migrateAPI.migrateScalacOption(filteredOptions.asJava)
        reportStatus(logger, migrationStatus)
      }
    }
  }

  private def reportStatus(logger: ManagedLogger, status: ScalacOptions): Unit = {
    val notParsed = status.getNotParsed.toSeq
    if (notParsed.nonEmpty) {
      logger.warn(s"""|
                      |We were not able to parse the following ScalacOptions:
                      |${formatScalacOptions(notParsed)}
                      |
                      |""".stripMargin)
    }

    val specific2 = status.getSpecificScala2.toSeq
    val scala3    = status.getScala3cOptions.toSeq
    val renamed   = status.getRenamed.asScala.toMap
    val plugins   = status.getPluginsOptions.toSeq
    logger.info(message(specific2, renamed, scala3, plugins))
  }

  private def starting(projectId: String, configs: Seq[String]): String =
    s"""|
        |${BOLD}Starting to migrate the scalacOptions in $projectId / [${configs.mkString(",")}]${RESET}
        |""".stripMargin

  private val warning: String =
    s"""|${YELLOW}Some scalacOptions are set by sbt plugins and don't need to be modified, removed or added.${RESET}
        |${YELLOW}The sbt plugin should adapt its own scalacOptions for Scala 3${RESET}""".stripMargin

  private val removedSign = s"""${BOLD}${RED}X${RESET}"""
  private val renamedSign = s"""${BOLD}${BLUE}Renamed${RESET}"""
  private val sameSign    = s"""${BOLD}${GREEN}Valid${RESET}"""
  private val pluginSign  = s"""${BOLD}${CYAN}Plugin${RESET}"""
  private val spacesHelp  = computeLongestValue(Seq(removedSign, renamedSign, sameSign, pluginSign))

  private val help =
    s"""
       |${formatValueWithSpace(removedSign, spacesHelp)} $RED: the option is not available is Scala 3$RESET
       |${formatValueWithSpace(renamedSign, spacesHelp)} $BLUE: the option has been renamed$RESET
       |${formatValueWithSpace(sameSign, spacesHelp)} $GREEN: the option is still valid$RESET
       |${formatValueWithSpace(pluginSign, spacesHelp)} $CYAN: the option is related to a plugin$RESET
       |
       |""".stripMargin

  private def message(
    removed: Seq[String],
    renamed: Map[String, String],
    valid: Seq[String],
    plugins: Seq[String]
  ): String = {
    val longest = computeLongestValue(removed ++ renamed.keys ++ valid)

    val formattedRemoved =
      removed.map(r => s"""${formatValueWithSpace(r, longest)} -> $removedSign""").mkString("\n")

    val formattedRenamed: String = renamed.map { case (initial, renamed) =>
      s"""${formatValueWithSpace(initial, longest)} -> ${BOLD}${BLUE}${renamed}${RESET}"""
    }.mkString("\n")

    val formattedValid: String =
      valid.map(r => s"""${formatValueWithSpace(r, longest)} -> $sameSign""").mkString("\n")

    val formattedPlugins: String = {
      val longest = computeLongestValue(plugins)
      plugins.map(r => s"""${formatValueWithSpace(r, longest)} -> $pluginSign""").mkString("\n")
    }

    Seq(formattedRemoved, formattedRenamed, formattedValid, formattedPlugins)
      .filterNot(_.isEmpty)
      .mkString("", "\n", "\n\n")
  }

  private def formatScalacOptions(l: Seq[String]): String =
    l.mkString("Seq(\n\"", "\",\n\"", "\"\n)")
}
