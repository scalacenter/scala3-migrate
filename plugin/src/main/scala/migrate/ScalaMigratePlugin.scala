package migrate

import migrate.CommandStrings._
import migrate.interfaces.Migrate
import sbt.BasicCommandStrings._
import sbt.Keys._
import sbt._
import sbt.internal.util.complete.Parser
import sbt.internal.util.complete.Parser.token
import sbt.internal.util.complete.Parsers.Space
import sbt.plugins.JvmPlugin

import java.nio.file.Path
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
  private[migrate] val syntheticsOn            = "-P:semanticdb:synthetics:on"
  private[migrate] val migrationOn             = "-source:3.0-migration"
  private[migrate] val scalaBinaryVersion      = BuildInfo.scalaBinaryVersion
  private[migrate] val migrateVersion          = BuildInfo.version
  private[migrate] val scala3Version           = BuildInfo.scala3Version
  private[migrate] val migrateScalametaVersion = BuildInfo.scalametaVersion
  private[migrate] val migrateAPI              = Migrate.fetchAndClassloadInstance(migrateVersion, scalaBinaryVersion)

  private[migrate] val inputsStore: mutable.Map[Scope, Scala2Inputs] = mutable.Map()

  private[migrate] object Keys {
    val scala2Version = AttributeKey[String]("scala2Version")

    val migrationConfigs =
      settingKey[List[Configuration]]("the ordered list of configurations to migrate").withRank(KeyRanks.Invisible)

    val scala2Inputs = taskKey[Scala2Inputs]("return Scala 2 inputs").withRank(KeyRanks.Invisible)
    val scala3Inputs = taskKey[Scala3Inputs]("return Scala 3 inputs").withRank(KeyRanks.Invisible)
    val storeScala2Inputs =
      taskKey[StateTransform]("store Scala 2 inputs from all migration configurations").withRank(KeyRanks.Invisible)

    val internalMigrateSyntax =
      taskKey[Unit]("fix some syntax incompatibilities with scala 3").withRank(KeyRanks.Invisible)
    val internalMigrateScalacOptions = taskKey[Unit]("migrate of the scalacOptions").withRank(KeyRanks.Invisible)
    val internalMigrateDependencies  = taskKey[Unit]("migrate dependencies").withRank(KeyRanks.Invisible)
    val internalMigrateTypes         = taskKey[Unit]("migrate types to scala 3").withRank(KeyRanks.Invisible)
  }

  import Keys._

  override def requires: Plugins = JvmPlugin

  override def trigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] = Def.settings(
    semanticdbEnabled := {
      val sv = scalaVersion.value
      if (sv.startsWith("2.13.")) true
      else semanticdbEnabled.value
    },
    semanticdbVersion := {
      val sv = scalaVersion.value
      if (sv.startsWith("2.13.")) {
        val actual = semanticdbVersion.value
        if (actual > migrateScalametaVersion) actual else migrateScalametaVersion
      } else semanticdbVersion.value
    },
    migrationConfigs                         := migrationConfigsImpl.value,
    migrationConfigs / aggregate             := false,
    storeScala2Inputs                        := storeScala2InputsImpl.value,
    storeScala2Inputs / aggregate            := false,
    internalMigrateScalacOptions             := ScalacOptionsMigration.internalImpl.value,
    internalMigrateScalacOptions / aggregate := false,
    internalMigrateSyntax                    := SyntaxMigration.internalImpl.value,
    internalMigrateSyntax / aggregate        := false,
    internalMigrateTypes                     := TypeInferenceMigration.internalImpl.value,
    internalMigrateTypes / aggregate         := false,
    internalMigrateDependencies              := LibsMigration.internalImpl.value,
    internalMigrateDependencies / aggregate  := false,
    commands ++= Seq(migrateSyntax, migrateScalacOptions, migrateLibDependencies, migrateTypes, fallback),
    inConfig(Compile)(configSettings),
    inConfig(Test)(configSettings)
  )

  val configSettings: Seq[Setting[_]] = Def.settings(
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

  private val storeScala2InputsImpl = Def.taskDyn {
    val configs    = migrationConfigs.value
    val projectRef = thisProjectRef.value
    val sv         = scalaVersion.value

    if (!sv.startsWith("2.13."))
      sys.error(Messages.notScala213(sv, projectRef.project))

    Def.task {
      val allScala2Inputs = configs.map(_ / scala2Inputs).join.value
      for {
        (scala2Inputs, config) <- allScala2Inputs.zip(configs)
      } {
        val scope = (projectRef / config / Keys.scala2Inputs).scope
        inputsStore.update(scope, scala2Inputs)
      }

      StateTransform(_.put(scala2Version, sv))
    }
  }

  /**
   * Return all configurations that can be migrated in a project. If config A extends config B then B appears first ex:
   * List(Compile, Test) because Test extends Runtime which extends Compile
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
      s"$projectId / internalMigrateDependencies" :: state
    }

  lazy val migrateTypes: Command =
    Command(migrateCommand, migrateBrief, migrateDetailed)(idParser) { (state, projectId) =>
      val commands = List(
        s"$projectId / storeScala2Inputs",
        setScalaVersion(projectId, scala3Version),
        StashOnFailure,                            // prepare onFailure
        s"$OnFailure $migrateFallback $projectId", // go back to Scala 2.13 in case of failure
        s"$projectId / internalMigrateTypes",
        FailureWall, // resume here in case of failure
        PopOnFailure // remove onFailure
      )
      commands ::: state
    }

  lazy val fallback: Command =
    Command(migrateFallback)(idParser) { (state, projectId) =>
      val scala2Version = state.attributes(Keys.scala2Version)
      setScalaVersion(projectId, scala2Version) :: state
    }

  private def setScalaVersion(projectId: String, scalaVersion: String): String =
    s"""set LocalProject("$projectId") / scalaVersion := "$scalaVersion""""

  private def sanitazeScala3Options(options: Seq[String]) = {
    val nonWorkingOptions = Set(syntheticsOn)
    options.filterNot(nonWorkingOptions)
  }
}
