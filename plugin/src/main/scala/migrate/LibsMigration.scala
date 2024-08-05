package migrate

import coursier.core.Repository
import ScalaMigratePlugin.Keys._
import lmcoursier.CoursierConfiguration
import lmcoursier.definitions.ToCoursier
import lmcoursier.internal.ResolutionParams
import Messages._
import migrate.internal.*
import sbt.Keys
import sbt.Def
import sbt.MessageOnlyException
import sbt.util.Logger

import scala.io.AnsiColor._
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

private[migrate] object LibsMigration {
  val internalImpl = Def.task {
    val log                 = Keys.streams.value.log
    val projectId           = Keys.thisProject.value.id
    val scalaVersion        = Keys.scalaVersion.value
    val libraryDependencies = Keys.libraryDependencies.value
    val csrConfig           = (Keys.updateClassifiers / Keys.csrConfiguration).value

    if (!scalaVersion.startsWith("2.13.") && !scalaVersion.startsWith("3."))
      throw new MessageOnlyException(notScala213(scalaVersion, projectId))

    log.info(startingMessage(projectId))

    val repositories = getCoursierRepositories(csrConfig, log)
    val migrateAPI   = ScalaMigratePlugin.getMigrateInstance(log)
    val migrated     = LibraryMigration.migrateLibs(libraryDependencies.map(InitialLib.apply), repositories)

    val validLibs = migrated.collect { case l: ValidLibrary => l }
    if (validLibs.nonEmpty) {
      log.info(validMessage(validLibs))
    }

    val updatedVersions = migrated.collect { case l: UpdatedVersion => l }
    if (updatedVersions.nonEmpty) {
      log.warn(updatedVersionsMessage(updatedVersions))
    }

    val crossCompatibleLibs = migrated.collect { case l: CrossCompatibleLibrary => l }
    if (crossCompatibleLibs.nonEmpty) {
      log.warn(crossCompatibleMessage(crossCompatibleLibs))
    }

    val integratedPlugins = migrated.collect { case l: IntegratedPlugin => l }
    if (integratedPlugins.nonEmpty) {
      log.warn(integratedPluginMessage(integratedPlugins))
    }

    val unclassifiedLibraries = migrated.collect { case l: UnclassifiedLibrary => l }
    if (unclassifiedLibraries.nonEmpty) {
      log.warn(unclassifiedMessage(unclassifiedLibraries))
    }

    val incompatibleLibraries = migrated.collect { case l: IncompatibleLibrary => l }
    if (incompatibleLibraries.nonEmpty) {
      log.error(incompatibleMessage(incompatibleLibraries))
    }

    log.info("\n")
  }

  private def startingMessage(projectId: String): String =
    s"""|
        |${BOLD}Starting migration of libraries and compiler plugins in project '$projectId'$RESET
        |""".stripMargin

  private def validMessage(validLibs: Seq[MigratedLib]): String =
    s"""|
        |$GREEN${BOLD}Valid dependencies:$RESET
        |${validLibs.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def updatedVersionsMessage(updatedVersions: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}Versions to update:$RESET
        |${updatedVersions.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def crossCompatibleMessage(crossCompatible: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}For Scala 3 use 2.13:$RESET
        |${crossCompatible.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def integratedPluginMessage(compilerPlugins: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}Integrated compiler plugins:$RESET
        |${compilerPlugins.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def unclassifiedMessage(unclassifiedLibraries: Seq[MigratedLib]): String =
    s"""|
        |$YELLOW${BOLD}Unclassified Libraries:$RESET
        |${unclassifiedLibraries.map(_.formatted).mkString("\n")}
        |""".stripMargin

  private def incompatibleMessage(incompatibleLibraries: Seq[MigratedLib]): String =
    s"""|
        |$RED${BOLD}Incompatible Libraries:$RESET
        |${incompatibleLibraries.map(_.formatted).mkString("\n")}
        |""".stripMargin

  // Copied from https://github.com/coursier/sbt-coursier/blob/5610ce56d6fcd9d716d817310be3ef4a2dfc9334/modules/lm-coursier/src/main/scala/lmcoursier/CoursierDependencyResolution.scala#L173-L193
  private def getCoursierRepositories(conf: CoursierConfiguration, log: Logger): Seq[Repository] = {
    val ivyProperties                = ResolutionParams.defaultIvyProperties(conf.ivyHome)
    val authenticationByRepositoryId = conf.authenticationByRepositoryId.toMap
    conf.resolvers.flatMap { resolver =>
      Resolvers.repository(
        resolver,
        ivyProperties,
        log,
        authenticationByRepositoryId.get(resolver.name).map(toCoursier),
        Seq.empty
      )
    }
  }

  private def toCoursier(authentication: lmcoursier.definitions.Authentication): coursier.core.Authentication =
    coursier.core
      .Authentication(authentication.user, authentication.password)
      .withOptional(authentication.optional)
      .withRealmOpt(authentication.realmOpt)
      .withHttpHeaders(authentication.headers)
      .withHttpsOnly(authentication.httpsOnly)
      .withPassOnRedirect(authentication.passOnRedirect)
}
