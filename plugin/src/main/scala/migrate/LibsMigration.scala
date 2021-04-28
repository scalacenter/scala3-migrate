package migrate

import ScalaMigratePlugin.{ migrateAPI, scala3Version }
import ScalaMigratePlugin.Keys._
import Messages._
import interfaceImpl.LibImpl
import migrate.interfaces.{ MigratedLibs, Lib }

import sbt.Keys._
import sbt._

import scala.io.AnsiColor._
import scala.util.{ Try, Success, Failure }
import scala.collection.JavaConverters._

private[migrate] object LibsMigration {
  val internalImpl = Def.task {
    val log       = streams.value.log
    val projectId = thisProject.value.id
    val sv        = scalaVersion.value

    if (!sv.startsWith("2.13."))
      throw new MessageOnlyException(notScala213(sv, projectId))

    log.info(welcomeMessage(projectId))

    val libDependencies: Seq[ModuleID] = libraryDependencies.value
    val libs                           = libDependencies.map(LibImpl)
    val migratedLibs: MigratedLibs     = migrateAPI.migrateLibs(libs.map(_.asInstanceOf[Lib]).asJava)
    val notMigrated                    = migratedLibs.getNotMigrated.toSeq
    val libsToUpdate = migratedLibs.getLibsToUpdate.asScala.toMap.map { case (lib, migrated) =>
      lib -> migrated.asScala
    }
    val validLibs                                        = migratedLibs.getValidLibs.toSeq
    val compilerPluginWithScalacOption: Map[Lib, String] = migratedLibs.getMigratedCompilerPlugins.asScala.toMap

    log.info(migrationMessage(notMigrated, validLibs, libsToUpdate, compilerPluginWithScalacOption))
  }

  private def welcomeMessage(projectId: String): String =
    s"""|
        |
        |${BOLD}Starting to migrate libDependencies for $projectId${RESET}
        |""".stripMargin

  private def migrationMessage(
    notMigrated: Seq[Lib],
    validLibs: Seq[Lib],
    toUpdate: Map[Lib, Seq[Lib]],
    compilerPluginsWithScalacOption: Map[Lib, String]
  ): String = {
    val removedSign = s"""${BOLD}${RED}X${RESET}"""
    val validSign   = s"""${BOLD}${CYAN}Valid${RESET}"""
    val toBeUpdated = s"""${BOLD}${BLUE}To be updated${RESET}"""
    val commentMacro =
      s"${BOLD}${YELLOW}Contains Macros and is not yet published for ${ScalaMigratePlugin.scala3Version}${RESET}"
    val commentCompilerPlugin =
      s"${BOLD}${YELLOW}Scala 2 compiler plugins are not supported in scala ${ScalaMigratePlugin.scala3Version}. You need to find an alternative${RESET}"
    val commentCompilerWithScalacOption =
      s"${BOLD}${YELLOW}This compiler plugin has a scalacOption equivalent. Add it to your scalacOptions$RESET"

    val spacesForLib = computeLongestValue(
      (notMigrated ++ validLibs ++ toUpdate.keys ++ compilerPluginsWithScalacOption.keys).map(_.toString)
    )

    val notMigratedWithComments =
      notMigrated.map(lib => if (lib.isCompilerPlugin) (lib, commentCompilerPlugin) else (lib, commentMacro))
    def formatCompilerPlugins: String =
      compilerPluginsWithScalacOption.map { case (l, scalacOption) =>
        format(l, Seq(scalacOption), spacesForLib) + s" : $commentCompilerWithScalacOption"
      }.mkString("\n")
    def formatNotMigrated: String = notMigratedWithComments.map { case (lib, comment) =>
      s"""${formatValueWithSpace(lib.toString, spacesForLib)} -> $removedSign : $comment"""
    }.mkString("\n")
    def formatValid: String =
      validLibs.map(lib => s"""${formatValueWithSpace(lib.toString, spacesForLib)} -> $validSign""").mkString("\n")

    val spacesForHelp = computeLongestValue(Seq(removedSign, validSign, toBeUpdated))

    val help = s"""
                  |${formatValueWithSpace(removedSign, spacesForHelp)} $RED: Cannot be updated to scala 3$RESET
                  |${formatValueWithSpace(validSign, spacesForHelp)} $CYAN: Already a valid version for Scala 3$RESET
                  |${formatValueWithSpace(toBeUpdated, spacesForHelp)} $BLUE: Need to be updated to the following version$RESET
                  |""".stripMargin

    Seq(help, formatNotMigrated, formatValid, formatLibs(toUpdate, spacesForLib), formatCompilerPlugins)
      .filterNot(_.isEmpty)
      .mkString("\n")
  }

  private def formatLibs(libs: Map[Lib, Seq[Lib]], longestValue: Int): String =
    libs.map { case (initial, migrated) => format(initial, migrated.map(_.toString), longestValue) }.mkString("\n")

  private def format(initial: Lib, migrated: Seq[String], longestValue: Int): String = {
    val numberOfSpaces = " " * (longestValue - initial.toString.length)
    s"""$initial$numberOfSpaces -> ${GREEN}${migrated.mkString(", ")}$RESET"""
  }
}
