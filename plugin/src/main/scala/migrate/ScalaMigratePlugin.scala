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

case class Scala3Inputs(
  projectId: String,
  scalaVerson: String,
  scalacOptions: Seq[String],
  classpath: Seq[Path],
  classDirectory: Path
)

case class Scala2Inputs(
  projectId: String,
  scalaVerson: String,
  scalacOptions: Seq[String],
  classpath: Seq[Path],
  unmanagedSources: Seq[Path],
  managedSources: Seq[Path]
)

object ScalaMigratePlugin extends AutoPlugin {
  private[migrate] val scala3inputsAttirbute    = AttributeKey[Scala3Inputs]("scala3Inputs")
  private[migrate] val scala2inputsAttirbute    = AttributeKey[Scala2Inputs]("scala2Inputs")
  private[migrate] val syntheticsOn             = "-P:semanticdb:synthetics:on"
  private[migrate] val migrationOn              = "-source:3.0-migration"
  private[migrate] val scalaBinaryVersion       = BuildInfo.scalaBinaryVersion
  private[migrate] val migrateVersion           = BuildInfo.version
  private[migrate] val scala3Version            = BuildInfo.scala3Version
  private[migrate] val migrateSemanticdbVersion = BuildInfo.semanticdbVersion
  private[migrate] val migrateAPI               = Migrate.fetchAndClassloadInstance(migrateVersion, scalaBinaryVersion)

  object autoImport {
    val scala3InputComputed = taskKey[Scala3Inputs]("show scala 3 inputs if available")
    val scala2InputComputed = taskKey[Scala2Inputs]("show scala 2 inputs if available")

    private[migrate] val storeScala3Inputs            = taskKey[StateTransform]("store scala 3 inputs")
    private[migrate] val storeScala2Inputs            = taskKey[StateTransform]("store scala 2 inputs")
    private[migrate] val internalMigrateSyntax        = taskKey[Unit]("fix some syntax incompatibilities with scala 3")
    private[migrate] val internalMigrateScalacOptions = taskKey[Unit]("log information to migrate scalacOptions")
    private[migrate] val internalMigrateLibs          = taskKey[Unit]("log information to migrate libDependencies")
    private[migrate] val internalMigrate              = taskKey[Unit]("migrate a specific project to scala 3")
    private[migrate] val isScala213                   = taskKey[Boolean]("is this project a scala 2.13 project")
  }

  import autoImport._

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
      }
    ) ++
      inConfig(Compile)(configSettings) ++
      inConfig(Test)(configSettings) ++
      Seq(commands ++= Seq(migrateSyntax, migrateScalacOptions, migrateLibDependencies, migrate))

  private def idParser(state: State): Parser[String] = {
    val projects           = Project.structure(state).allProjects.map(_.id)
    val projectCompletions = projects.map(token(_)).reduce(_ | _)
    Space ~> projectCompletions
  }

  /**
   * Return all configurations that can be migrated in a project.
   * If config A extends config B then B appears first
   * ex: List(Compile, Test) because Test extends Runtime which extends Compile
   */
  private def allMigrationConfigs(state: State, projectId: String): List[Configuration] = {
    val project = Project.structure(state).allProjects.find(p => p.id == projectId).get
    val migrationConfigs: Set[String] = (
      for {
        setting   <- project.settings if setting.key.key.label == internalMigrate.key.label
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

  private def onAllMigrationConfigs(state: State, projectId: String)(tasks: TaskKey[_]*): List[String] =
    for {
      config <- allMigrationConfigs(state, projectId)
      task   <- tasks
    } yield s"$projectId / ${config.id} / ${task.key.label}"

  lazy val migrateSyntax: Command =
    Command(migrateSyntaxCommand, migrateSyntaxBrief, migrateSyntaxDetailed)(idParser) { (state, projectId) =>
      val commands = List(StashOnFailure, s"$projectId / isScala213") ++ onAllMigrationConfigs(state, projectId)(
        compile,
        storeScala2Inputs,
        internalMigrateSyntax
      ) ++ List(PopOnFailure)
      commands ::: state
    }

  lazy val migrateScalacOptions: Command =
    Command(migrateScalacOptionsCommand, migrateScalacOptionsBrief, migrateScalacOptionsDetailed)(idParser) {
      (state, projectId) =>
        val commands =
          s"${projectId} / isScala213" ::
            onAllMigrationConfigs(state, projectId)(internalMigrateScalacOptions)
        commands ::: state
    }

  lazy val migrateLibDependencies: Command =
    Command(migrateLibs, migrateLibsBrief, migrateLibsDetailed)(idParser) { (state, projectId) =>
      val commands = List(s"$projectId / isScala213", s"$projectId / internalMigrateLibs")
      commands ::: state
    }

  lazy val migrate: Command =
    Command(migrateCommand, migrateBrief, migrateDetailed)(idParser) { (state, projectId) =>
      val commands = List(StashOnFailure, s"${projectId} / isScala213") ++ onAllMigrationConfigs(state, projectId)(
        compile,
        storeScala2Inputs
      ) ++ List(s"""set LocalProject("$projectId") / scalaVersion := "$scala3Version"""") ++ onAllMigrationConfigs(
        state,
        projectId
      )(storeScala3Inputs, internalMigrate) ++ List(PopOnFailure)
      commands ::: state
    }

  val configSettings: Seq[Setting[_]] =
    Seq(
      isScala213 := {
        val sv = scalaVersion.value
        if (sv.startsWith("2.13.")) true
        else throw new Exception(Messages.notScala213(sv, thisProject.value.id))
      },
      isScala213 / aggregate := false,
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
      internalMigrateSyntax := migrateSyntaxImpl.value,
      internalMigrateSyntax / aggregate := false,
      internalMigrateScalacOptions := migrateScalacOptionsImp.value,
      internalMigrateScalacOptions / aggregate := false,
      internalMigrateLibs := internalMigrateLibsImp.value,
      internalMigrateLibs / aggregate := false,
      internalMigrate := migrateImp.value,
      internalMigrate / aggregate := false,
      scala3InputComputed := {
        (for {
          inputs <- state.value.attributes.get(scala3inputsAttirbute)
        } yield Scala3Inputs(
          inputs.projectId,
          inputs.scalaVerson,
          inputs.scalacOptions,
          inputs.classpath,
          inputs.classDirectory
        )).get

      },
      scala3InputComputed / aggregate := false,
      scala2InputComputed := {
        (for {
          inputs <- state.value.attributes.get(scala2inputsAttirbute)
        } yield Scala2Inputs(
          inputs.projectId,
          inputs.scalaVerson,
          inputs.scalacOptions,
          inputs.classpath,
          inputs.unmanagedSources,
          inputs.managedSources
        )).get
      },
      scala2InputComputed / aggregate := false,
      storeScala3Inputs := {
        val projectId            = thisProject.value.id
        val scalaVersion         = Keys.scalaVersion.value
        val sOptions             = scalacOptions.value
        val classpath            = dependencyClasspath.value.map(_.data.toPath())
        val scala3Lib            = scalaInstance.value.libraryJars.toSeq.map(_.toPath)
        val scala3ClassDirectory = (compile / classDirectory).value.toPath
        val scalac3Options       = sanitazeScala3Options(sOptions)
        val scala3Inputs =
          Scala3Inputs(projectId, scalaVersion, scalac3Options, scala3Lib ++ classpath, scala3ClassDirectory)
        StateTransform(s => s.put(scala3inputsAttirbute, scala3Inputs))
      },
      storeScala3Inputs / aggregate := false,
      storeScala2Inputs := {
        val projectId    = thisProject.value.id
        val scalaVersion = Keys.scalaVersion.value
        val sOptions     = scalacOptions.value
        val classpath    = fullClasspath.value.map(_.data.toPath())
        val unmanaged    = Keys.unmanagedSources.value.map(_.toPath())
        val managed      = Keys.managedSources.value.map(_.toPath())
        val scala2Inputs = Scala2Inputs(projectId, scalaVersion, sOptions, classpath, unmanaged, managed)
        StateTransform(s => s.put(scala2inputsAttirbute, scala2Inputs))
      },
      storeScala2Inputs / aggregate := false
    )

  def migrateSyntaxImpl = Def.task {
    val log        = streams.value.log
    val targetRoot = semanticdbTargetRoot.value
    val projectId  = thisProject.value.id
    log.info(Messages.welcomeMigrateSyntax(projectId))
    // computed values
    val scala2InputsValue     = state.value.attributes.get(scala2inputsAttirbute).get
    val unamangedSources      = scala2InputsValue.unmanagedSources
    val scala2Classpath       = scala2InputsValue.classpath
    val scala2CompilerOptions = scala2InputsValue.scalacOptions

    Try {
      migrateAPI.migrateSyntax(
        unamangedSources.asJava,
        targetRoot.toPath,
        scala2Classpath.asJava,
        scala2CompilerOptions.asJava
      )
    } match {
      case Success(_) =>
        log.info(Messages.successMessageMigrateSyntax(projectId, scala3Version))
      case Failure(exception) =>
        log.err(Messages.errorMessageMigrateSyntax(projectId, exception))
    }
  }

  def migrateScalacOptionsImp = Def.task {
    val log       = streams.value.log
    val projectId = thisProject.value.id
    log.info(Messages.migrationScalacOptionsStarting(projectId))
    log.warn(Messages.warnMessageScalacOption)

    val scalacOptions2 = scalacOptions.value
    val migrated       = migrateAPI.migrateScalacOption(scalacOptions2.asJava)
    val notParsed      = migrated.getNotParsed.toSeq
    val specific2      = migrated.getSpecificScala2.toSeq
    val scala3         = migrated.getScala3cOptions.toSeq
    val renamed        = migrated.getRenamed.asScala.toMap
    val plugins        = migrated.getPluginsOptions.toSeq

    Messages.notParsed(notParsed).foreach(message => log.warn(message))
    log.info(Messages.scalacOptionsMessage(specific2, renamed, scala3, plugins))
  }

  def internalMigrateLibsImp = Def.task {
    val log       = streams.value.log
    val projectId = thisProject.value.id
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

  def migrateImp =
    Def.task {
      val log       = streams.value.log
      val projectID = thisProject.value.id
      log.info(Messages.welcomeMigration(projectID))

      val targetRoot = semanticdbTargetRoot.value

      // computed values
      val scala2InputsValue     = state.value.attributes.get(scala2inputsAttirbute).get
      val scala2Classpath       = scala2InputsValue.classpath
      val scala2CompilerOptions = scala2InputsValue.scalacOptions
      val unamangedSources      = scala2InputsValue.unmanagedSources
      val managedSources        = scala2InputsValue.managedSources

      val scala3InputsValue = state.value.attributes.get(scala3inputsAttirbute).get
      val scalac3Options    = scala3InputsValue.scalacOptions
      val scala3Classpath   = scala3InputsValue.classpath
      val scala3ClassDir    = scala3InputsValue.classDirectory
      if (!Files.exists(scala3ClassDir)) Files.createDirectory(scala3ClassDir)

      Try {
        migrateAPI.migrate(
          unamangedSources.asJava,
          managedSources.asJava,
          targetRoot.toPath(),
          scala2Classpath.asJava,
          scala2CompilerOptions.asJava,
          scala3Classpath.asJava,
          scalac3Options.asJava,
          scala3ClassDir
        )
      } match {
        case Success(_) =>
          log.info(Messages.successOfMigration(projectID, scala3Version))

        case Failure(_: CompilationException) =>
          log.err(Messages.errorMesssageMigration(None))
        case Failure(exception) =>
          log.err(Messages.errorMesssageMigration(Some(exception)))
      }
    }

  private def sanitazeScala3Options(options: Seq[String]) = {
    val nonWorkingOptions = Set(syntheticsOn)
    options.filterNot(nonWorkingOptions)
  }
}
