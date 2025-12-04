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
import scala.Console._

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
  private val syntheticsOn = "-P:semanticdb:synthetics:on"
  private val migrationOn  = "-source:3.0-migration"
  private val classLoader  = Migrate.getClassLoader(BuildInfo.version, BuildInfo.scalaBinaryVersion)

  private[migrate] def getMigrateInstance(logger: Logger) = {
    val migrateLogger = new ScalaMigrateLogger(logger)
    Migrate.getInstance(classLoader, migrateLogger)
  }

  private[migrate] val inputsStore: mutable.Map[Scope, Scala2Inputs] = mutable.Map()

  object Keys {
    val scala2Version = AttributeKey[String]("scala2Version")

    val migrationConfigs =
      settingKey[List[Configuration]]("the ordered list of configurations to migrate").withRank(KeyRanks.Invisible)

    val scala2Inputs      = taskKey[Scala2Inputs]("return Scala 2 inputs").withRank(KeyRanks.Invisible)
    val scala3Inputs      = taskKey[Scala3Inputs]("return Scala 3 inputs").withRank(KeyRanks.Invisible)
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

  override def globalSettings: Seq[Setting[_]] = Def.settings(
    onLoad := {
      val previousOnLoad = onLoad.value
      state0 => {
        val state1 = previousOnLoad(state0)
        if (state1.currentCommand.exists(e => e.commandLine == "loadp")) {
          state1.log.info(
            s"""|
                |$GREEN${BOLD}sbt-scala3-migrate ${BuildInfo.version} detected!$RESET
                |It can assist you during the migration to Scala 3.
                |Run the following commands in order, to start migrating to Scala 3:
                |  1. ${BOLD}migrateDependencies <project>$RESET
                |  2. ${BOLD}migrateScalacOptions <project>$RESET
                |  3. ${BOLD}migrateSyntax <project>$RESET
                |  4. ${BOLD}migrateTypes <project>$RESET
                |Learn more about them on https://docs.scala-lang.org/scala3/guides/migration/scala3-migrate.html
                |Remove sbt-scala3-migrate from your project/plugins.sbt to clear this message out.
                |
                |""".stripMargin
          )
        }
        state1
      }
    },
    commands ++= Seq(
      migrateSyntax,
      migrateScalacOptions,
      migrateLibDependencies,
      migrateTypes,
      internalMigrateFallback,
      internalMigrateFallbackAndFail,
      internalMigrateFail
    )
  )

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
        if (actual > BuildInfo.scalametaVersion) actual else BuildInfo.scalametaVersion
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
    commands ++= Seq(
      migrateSyntax,
      migrateScalacOptions,
      migrateLibDependencies,
      migrateTypes,
      internalMigrateFallback,
      internalMigrateFallbackAndFail,
      internalMigrateFail),
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
      val scalac3Options       = sanitizeScala3Options(sOptions)
      val semanticdbTarget     = semanticdbTargetRoot.value.toPath
      Scala3Inputs(projectId, sv, scalac3Options, scala3Lib ++ classpath, scala3ClassDirectory, semanticdbTarget)
    },
    scala3Inputs / aggregate := false,
    scala2Inputs             := {
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
    val project                       = thisProject.value
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
      val preparedState = state.copy(attributes = state.attributes.remove(Keys.scala2Version))
      val commands      = List(
        StashOnFailure, // stash shell from onFailure
        s"$OnFailure internalMigrateFallbackAndFail $projectId",
        s"$projectId / storeScala2Inputs",
        setScalaVersion(projectId, BuildInfo.scala3Version), // set Scala 3
        s"$projectId / internalMigrateTypes",
        PopOnFailure,                         // pop shell to onFailure in case the fallback fails
        s"internalMigrateFallback $projectId" // set Scala 2.13
      )
      commands ::: preparedState
    }

  lazy val internalMigrateFallback: Command =
    Command("internalMigrateFallback")(idParser) { (state, projectId) =>
      state.attributes.get(Keys.scala2Version) match {
        case Some(scala2Version) => setScalaVersion(projectId, scala2Version) :: state
        case None                => state
      }
    }

  lazy val internalMigrateFallbackAndFail: Command =
    Command("internalMigrateFallbackAndFail")(idParser) { (state, projectId) =>
      PopOnFailure :: s"internalMigrateFallback $projectId" :: s"internalMigrateFail $projectId" :: Nil ::: state
    }

  lazy val internalMigrateFail: Command =
    Command("internalMigrateFail")(idParser) { (state, projectId) =>
      state.log.error(s"Migration of $projectId failed.")
      state.fail
    }

  private def setScalaVersion(projectId: String, scalaVersion: String): String =
    s"""set LocalProject("$projectId") / scalaVersion := "$scalaVersion""""

  private def sanitizeScala3Options(options: Seq[String]) = {
    val nonWorkingOptions = Set(syntheticsOn)
    options.filterNot(nonWorkingOptions)
  }
}
