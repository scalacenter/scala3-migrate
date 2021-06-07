package migrate

import ScalaMigratePlugin.{ migrateAPI, scala3Version }
import ScalaMigratePlugin.Keys._
import Messages._
import interfaceImpl.LibImpl
import migrate.interfaces.{ Lib, MigratedLib, MigratedLibs }
import sbt.Keys._
import sbt._

import scala.io.AnsiColor._
import scala.util.{ Failure, Success, Try }
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
    val notMigrated                    = migratedLibs.getUncompatibleWithScala3.toSeq
    val libsToUpdate                   = migratedLibs.getLibsToUpdate.asScala.toMap

    val validLibs = migratedLibs.getValidLibs.toSeq

    log.info(migrationMessage(notMigrated, validLibs, libsToUpdate))
  }

  private def welcomeMessage(projectId: String): String =
    s"""|
        |
        |${BOLD}Starting to migrate libDependencies for $projectId${RESET}
        |""".stripMargin

  private def migrationMessage(
    incompatibleLibs: Seq[MigratedLib],
    validLibs: Seq[MigratedLib],
    toUpdate: Map[Lib, MigratedLib]
  ): String = {
    val removedSign = s"""${BOLD}${RED}X${RESET}"""
    val validSign   = s"""${BOLD}${CYAN}Valid${RESET}"""
    val toBeUpdated = s"""${BOLD}${BLUE}To be updated${RESET}"""

    val spacesForLib                = computeLongestValue((incompatibleLibs ++ validLibs ++ toUpdate.keys).map(_.toString))
    def reasonWhy(lib: MigratedLib) = if (lib.getReasonWhy.isEmpty) "" else s": ${YELLOW}${lib.getReasonWhy}${RESET}"

    def formatIncompatibleLibs: String = incompatibleLibs.map { lib =>
      s"""${formatValueWithSpace(lib.toString, spacesForLib)} -> $removedSign ${reasonWhy(lib)}"""
    }.mkString("\n")

    def formatValid: String =
      validLibs.map { lib =>
        s"""${formatValueWithSpace(lib.toString, spacesForLib)} -> $validSign ${reasonWhy(lib)}"""
      }
        .mkString("\n")

    def formatLibToUpdate: String =
      toUpdate.map { case (initial, migrated) =>
        s"""${formatValueWithSpace(initial.toString, spacesForLib)} -> ${BLUE}${migrated.toString}$RESET ${reasonWhy(
          migrated
        )}"""
      }
        .mkString("\n")

    val spacesForHelp = computeLongestValue(Seq(removedSign, validSign, toBeUpdated))

    val help = s"""
                  |${formatValueWithSpace(removedSign, spacesForHelp)} $RED: Cannot be updated to scala 3$RESET
                  |${formatValueWithSpace(validSign, spacesForHelp)} $CYAN: Already a valid version for Scala 3$RESET
                  |${formatValueWithSpace(toBeUpdated, spacesForHelp)} $BLUE: Need to be updated to the following version$RESET
                  |""".stripMargin

    Seq(help, formatIncompatibleLibs, formatValid, formatLibToUpdate)
      .filterNot(_.isEmpty)
      .mkString("\n")
  }

}
