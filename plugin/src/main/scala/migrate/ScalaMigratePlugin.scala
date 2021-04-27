package migrate

import interfaceImpl.LibImpl
import migrate.CommandStrings._
import migrate.interfaces.{ CompilationException, Lib, Migrate, MigratedLibs }
import sbt.BasicCommandStrings._
import sbt.Keys._
import sbt._
import sbt.internal.util.complete.Parser
import sbt.internal.util.complete.Parser.token
import sbt.internal.util.complete.Parsers.Space
import sbt.plugins.JvmPlugin

import java.nio.file.{ Files, Path }
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }
import scala.collection.mutable

case class Scala3Inputs(
  projectId: String,
  scalaVerson: String,
  scalacOptions: Seq[String],
  classpath: Seq[Path],
  classDirectory: Path,
  semanticdbTarget: Path
)

case class Scala2Inputs(
  projectId: String,
  scalaVerson: String,
  scalacOptions: Seq[String],
  classpath: Seq[Path],
  unmanagedSources: Seq[Path],
  managedSources: Seq[Path],
  semanticdbTarget: Path
)

object ScalaMigratePlugin extends AutoPlugin {
  private[migrate] val syntheticsOn             = "-P:semanticdb:synthetics:on"
  private[migrate] val migrationOn              = "-source:3.0-migration"
  private[migrate] val scalaBinaryVersion       = BuildInfo.scalaBinaryVersion
  private[migrate] val migrateVersion           = BuildInfo.version
  private[migrate] val scala3Version            = BuildInfo.scala3Version
  private[migrate] val migrateSemanticdbVersion = BuildInfo.semanticdbVersion
  private[migrate] val migrateAPI               = Migrate.fetchAndClassloadInstance(migrateVersion, scalaBinaryVersion)

  private[migrate] val inputsStore: mutable.Map[Scope, Scala2Inputs] = mutable.Map()
  private[migrate] object Keys {
    val migrationConfigs = settingKey[List[Configuration]]("the ordered list of configuration to migrate")

    val scala2Inputs = taskKey[Scala2Inputs]("return Scala 2 inputs")
    val scala3Inputs = taskKey[Scala3Inputs]("return Scala 3 inputs")

    val storeScala2Inputs            = taskKey[Unit]("store Scala 2 inputs from all migration configurations")
    val internalMigrateSyntax        = taskKey[Unit]("fix some syntax incompatibilities with scala 3")
    val internalMigrateScalacOptions = taskKey[Unit]("log information about migratin of the scalacOptions")
    val internalMigrateLibs          = taskKey[Unit]("log information to migrate libDependencies")
    val internalMigrate              = taskKey[Unit]("migrate a specific project to scala 3")
  }

  import Keys._

  override def requires: Plugins = JvmPlugin

  override def trigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      semanticdbEnabled := {
        val sv = scalaVersion.value
        if (sv.startsWith("2.13.")) true
        else semanticdbEnabled.value
      },
      semanticdbVersion := {
        val sv = scalaVersion.value
        if (sv.startsWith("2.13.")) migrateSemanticdbVersion
        else semanticdbVersion.value
      },
      migrationConfigs := migrationConfigsImpl.value,
      migrationConfigs / aggregate := false,
      storeScala2Inputs := storeScala2InputsImpl.value,
      storeScala2Inputs / aggregate := false,
      internalMigrateScalacOptions := ScalacOptionsMigration.internalImpl.value,
      internalMigrateScalacOptions / aggregate := false,
      internalMigrateSyntax := SyntaxMigration.internalImpl.value,
      internalMigrateSyntax / aggregate := false,
      internalMigrate := TypeInferenceMigration.internalImpl.value,
      internalMigrate / aggregate := false
    ) ++
      inConfig(Compile)(configSettings) ++
      inConfig(Test)(configSettings) ++
      Seq(commands ++= Seq(migrateSyntax, migrateScalacOptions, migrateLibDependencies, migrate))

  private val storeScala2InputsImpl = Def.taskDyn {
    val configs    = migrationConfigs.value
    val projectRef = thisProjectRef.value
    val sv         = scalaVersion.value

    if (!sv.startsWith("2.13."))
      sys.error(Messages.notScala213(sv, projectRef.project))

    val filter = ScopeFilter(configurations = inConfigurations(configs: _*))
    Def.task {
      val allScala2Inputs = scala2Inputs.all(filter).value
      for {
        (scala2Inputs, config) <- allScala2Inputs.zip(configs)
      } {
        val scope = (projectRef / config / Keys.scala2Inputs).scope
        inputsStore.update(scope, scala2Inputs)
      }
    }
  }

  /**
   * Return all configurations that can be migrated in a project.
   * If config A extends config B then B appears first
   * ex: List(Compile, Test) because Test extends Runtime which extends Compile
   */
  private def migrationConfigsImpl = Def.setting {
    val project = thisProject.value
    val migrationConfigs: Set[String] = (
      for {
        setting   <- project.settings if setting.key.key.label == scala3Inputs.key.label
        configKey <- setting.key.scope.config.toOption
      } yield configKey.name
    ).toSet

    // if A extends B then B is added first, ex: List(Test, Runtime, Compile)
    def add(allConfigs: List[Configuration], config: Configuration): List[Configuration] =
      if (allConfigs.exists(_ == config)) allConfigs
      else config :: config.extendsConfigs.foldLeft(allConfigs)(add)

    val orderedConfigs = project.configurations.foldLeft(List.empty[Configuration])(add)
    orderedConfigs.filter(c => migrationConfigs.contains(c.name)).reverse
  }

  private def idParser(state: State): Parser[String] = {
    val projects           = Project.structure(state).allProjects.map(_.id)
    val projectCompletions = projects.map(token(_)).reduce(_ | _)
    Space ~> projectCompletions
  }

  lazy val migrateSyntax: Command =
    Command(migrateSyntaxCommand, migrateSyntaxBrief, migrateSyntaxDetailed)(idParser) { (state, projectId) =>
      s"$projectId / internalMigrateSyntax" :: state
    }

  lazy val migrateScalacOptions: Command =
    Command(migrateScalacOptionsCommand, migrateScalacOptionsBrief, migrateScalacOptionsDetailed)(idParser) {
      (state, projectId) =>
        s"$projectId / internalMigrateScalacOptions" :: state
    }

  lazy val migrateLibDependencies: Command =
    Command(migrateLibs, migrateLibsBrief, migrateLibsDetailed)(idParser) { (state, projectId) =>
      s"$projectId / internalMigrateLibs" :: state
    }

  lazy val migrate: Command =
    Command(migrateCommand, migrateBrief, migrateDetailed)(idParser) { (state, projectId) =>
      val commands = List(
        StashOnFailure,
        s"$projectId / storeScala2Inputs",
        s"""set LocalProject("$projectId") / scalaVersion := "$scala3Version"""",
        s"$projectId / internalMigrate",
        PopOnFailure
      )
      commands ::: state
    }

  val configSettings: Seq[Setting[_]] =
    Seq(
      scalacOptions ++= {
        val sv       = scalaVersion.value
        val settings = scalacOptions.value
        if (
          sv.startsWith("2.13.") && semanticdbEnabled.value && !settings
            .contains(syntheticsOn)
        )
          Seq(syntheticsOn)
        else if (sv.startsWith("3.") && !settings.contains(migrationOn))
          Seq(migrationOn)
        else Nil
      },
      internalMigrateLibs := internalMigrateLibsImp.value,
      internalMigrateLibs / aggregate := false,
      scala3Inputs := {
        val projectId            = thisProject.value.id
        val sv                   = scalaVersion.value
        val sOptions             = scalacOptions.value
        val classpath            = dependencyClasspath.value.map(_.data.toPath())
        val scala3Lib            = scalaInstance.value.libraryJars.toSeq.map(_.toPath)
        val scala3ClassDirectory = (compile / classDirectory).value.toPath
        val scalac3Options       = sanitazeScala3Options(sOptions)
        val semanticdbTarget     = semanticdbTargetRoot.value.toPath
        Scala3Inputs(projectId, sv, scalac3Options, scala3Lib ++ classpath, scala3ClassDirectory, semanticdbTarget)
      },
      scala3Inputs / aggregate := false,
      scala2Inputs := {
        val projectId        = thisProject.value.id
        val sv               = scalaVersion.value
        val sOptions         = scalacOptions.value
        val classpath        = fullClasspath.value.map(_.data.toPath())
        val unmanaged        = unmanagedSources.value.map(_.toPath())
        val managed          = managedSources.value.map(_.toPath())
        val semanticdbTarget = semanticdbTargetRoot.value.toPath
        Scala2Inputs(projectId, sv, sOptions, classpath, unmanaged, managed, semanticdbTarget)
      },
      scala2Inputs / aggregate := false
    )

  def internalMigrateLibsImp = Def.task {
    val log       = streams.value.log
    val projectId = thisProject.value.id
    val sv        = scalaVersion.value

    if (!sv.startsWith("2.13."))
      sys.error(Messages.notScala213(sv, projectId))

    log.info(Messages.migrateLibsStarting(projectId))

    val libDependencies: Seq[ModuleID] = libraryDependencies.value
    val libs                           = libDependencies.map(LibImpl)
    val migratedLibs: MigratedLibs     = migrateAPI.migrateLibs(libs.map(_.asInstanceOf[Lib]).asJava)
    // to scala Seq
    val notMigrated = migratedLibs.getNotMigrated.toSeq
    val libsToUpdate = migratedLibs.getLibsToUpdate.asScala.toMap.map { case (lib, migrated) =>
      lib -> migrated.asScala
    }
    val validLibs                                        = migratedLibs.getValidLibs.toSeq
    val compilerPluginWithScalacOption: Map[Lib, String] = migratedLibs.getMigratedCompilerPlugins.asScala.toMap
    // logging

    log.info(Messages.messageForLibs(notMigrated, validLibs, libsToUpdate, compilerPluginWithScalacOption))
  }

  private def sanitazeScala3Options(options: Seq[String]) = {
    val nonWorkingOptions = Set(syntheticsOn)
    options.filterNot(nonWorkingOptions)
  }
}
