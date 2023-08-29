package migrate

import ScalaMigratePlugin.scala3Version
import ScalaMigratePlugin.Keys._
import Messages._
import migrate.interfaces.{Lib, MigratedLib, MigratedLibs}
import sbt.Keys
import sbt.Def
import sbt.MessageOnlyException

import scala.io.AnsiColor._
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

private[migrate] object LibsMigration {
  val internalImpl = Def.task {
    val log                 = Keys.streams.value.log
    val projectId           = Keys.thisProject.value.id
    val scalaVersion        = Keys.scalaVersion.value
    val libraryDependencies = Keys.libraryDependencies.value

    if (!scalaVersion.startsWith("2.13.") && !scalaVersion.startsWith("3."))
      throw new MessageOnlyException(notScala213(scalaVersion, projectId))

    log.info(startingMessage(projectId))

    val migrateAPI = ScalaMigratePlugin.getMigrateInstance(log)
    val migrated   = migrateAPI.migrateLibs(libraryDependencies.map(LibImpl.apply).asJava)

    val validLibs = migrated.getValidLibraries
    if (validLibs.nonEmpty) {
      log.info(validMessage(validLibs))
    }

    val updatedVersions = migrated.getUpdatedVersions
    if (updatedVersions.nonEmpty) {
      log.warn(updatedVersionsMessage(updatedVersions))
    }

    val crossCompatibleLibs = migrated.getCrossCompatibleLibraries
    if (crossCompatibleLibs.nonEmpty) {
      log.warn(crossCompatibleMessage(crossCompatibleLibs))
    }

    val integratedPlugins = migrated.getIntegratedPlugins
    if (integratedPlugins.nonEmpty) {
      log.warn(integratedPluginMessage(integratedPlugins))
    }

    val unclassifiedLibraries = migrated.getUnclassifiedLibraries
    if (unclassifiedLibraries.nonEmpty) {
      log.warn(unclassifiedMessage(unclassifiedLibraries))
    }

    val incompatibleLibraries = migrated.getIncompatibleLibraries
    if (incompatibleLibraries.nonEmpty) {
      log.error(incompatibleMessage(incompatibleLibraries))
    }

    log.info("\n")
  }

  private def startingMessage(projectId: String): String =
    s"""|
        |${BOLD}Starting migration of libraries and compiler plugins of project $projectId${RESET}
        |""".stripMargin

  private def validMessage(validLibs: Seq[MigratedLib]): String =
    s"""|
        |$GREEN${BOLD}Valid dependencies:${RESET}
        |${validLibs.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def updatedVersionsMessage(updatedVersions: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}Versions to update:${RESET}
        |${updatedVersions.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def crossCompatibleMessage(crossCompatible: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}For Scala 3 use 2.13:${RESET}
        |${crossCompatible.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def integratedPluginMessage(compilerPlugins: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}Integrated compiler plugins:${RESET}
        |${compilerPlugins.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def unclassifiedMessage(unclassifiedLibraries: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}Unclassified Libraries:${RESET}
        |${unclassifiedLibraries.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def incompatibleMessage(incompatibleLibraries: Seq[MigratedLib]): String =
    s"""|
        |$RED${BOLD}Incompatible Libraries:${RESET}
        |${incompatibleLibraries.map(_.formatted).mkString("\n")}
        |""".stripMargin
}
