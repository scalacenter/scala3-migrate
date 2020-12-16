package migrate

import java.nio.file.{ Files, Path }

import buildinfo.BuildInfo
import migrate.interfaces.Migrate
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

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
  private[migrate] val scala3inputsAttirbute = AttributeKey[Scala3Inputs]("scala3Inputs")
  private[migrate] val scala2inputsAttirbute = AttributeKey[Scala2Inputs]("scala2Inputs")
  private[migrate] val syntheticsOn          = "-P:semanticdb:synthetics:on"
  private[migrate] val migrationOn           = "-source:3.0-migration"
  private[migrate] val scalaBinaryVersion    = BuildInfo.scalaBinaryVersion
  private[migrate] val migrateVersion        = BuildInfo.version
  private[migrate] val scala3Version         = BuildInfo.scala3Version
  private[migrate] val migrateAPI            = Migrate.fetchAndClassloadInstance(migrateVersion, scalaBinaryVersion)

  object autoImport {
    val scala3InputComputed = taskKey[Scala3Inputs]("show scala 3 inputs if available")
    val scala2InputComputed = taskKey[Scala2Inputs]("show scala 2 inputs if available")

    private[migrate] val storeScala3Inputs        = taskKey[StateTransform]("store scala 3 inputs")
    private[migrate] val storeScala2Inputs        = taskKey[StateTransform]("store scala 2 inputs")
    private[migrate] val internalPrepareMigration = taskKey[Unit]("fix some syntax incompatibilities with scala 3")
    private[migrate] val internalMigrate          = taskKey[Unit]("migrate a specific project to scala 3")
    private[migrate] val isScala213               = taskKey[Boolean]("is this project a scala 2.13 project")
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def trigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] =
    Seq(semanticdbEnabled := {
      if (scalaVersion.value.contains("2.13.")) true
      else semanticdbEnabled.value
    }) ++
      inConfig(Compile)(configSettings) ++
      inConfig(Test)(configSettings) ++
      Seq(commands ++= Seq(prepapreMigrateCommand, migrateCommand))

  lazy val migrateCommand: Command =
    Command.single("migrate") { (state, projectId) =>
      import sbt.BasicCommandStrings._

      val result = List(
        StashOnFailure,
        s"${projectId} / isScala213",
        s"$projectId / compile",
        s"$projectId / storeScala2Inputs",
        s"""set $projectId / scalaVersion := "${scala3Version}"""",
        s"$projectId / storeScala3Inputs",
        s"$projectId / internalMigrate",
        PopOnFailure,
        FailureWall
      ) ::: state
      result
    }

  lazy val prepapreMigrateCommand: Command =
    Command.single("migrate-prepare") { (state, projectId) =>
      import sbt.BasicCommandStrings._

      val result = List(
        StashOnFailure,
        s"${projectId} / isScala213",
        s"$projectId / compile",
        s"$projectId / storeScala2Inputs",
        s"$projectId / internalPrepareMigration",
        PopOnFailure,
        FailureWall
      ) ::: state
      result
    }

  val configSettings: Seq[Setting[_]] =
    Seq(
      isScala213 := {
        if (scalaVersion.value.startsWith("2.13.")) true
        else throw new Exception(Messages.notScala213(scalaVersion.value, thisProject.value.id))

      },
      scalacOptions ++= {
        if (scalaVersion.value.startsWith("2.13.") && !scalacOptions.value.contains(syntheticsOn))
          Seq(syntheticsOn)
        else if (scalaVersion.value.startsWith("3.") && !scalacOptions.value.contains(migrationOn))
          Seq(migrationOn)
        else Nil
      },
      internalPrepareMigration := prepareMigrateImpl.value,
      internalMigrate := migrateImp.value,
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
      storeScala3Inputs := {
        val projectId            = thisProject.value.id
        val scalaVersion         = Keys.scalaVersion.value
        val sOptions             = scalacOptions.value
        val classpath            = managedClasspath.value.map(_.data.toPath())
        val scala3ClassDirectory = (compile / classDirectory).value.toPath
        val scalac3Options       = sanitazeScala3Options(sOptions)
        val scala3Inputs         = Scala3Inputs(projectId, scalaVersion, scalac3Options, classpath, scala3ClassDirectory)
        StateTransform(s => s.put(scala3inputsAttirbute, scala3Inputs))
      },
      storeScala2Inputs := {
        val projectId    = thisProject.value.id
        val scalaVersion = Keys.scalaVersion.value
        val sOptions     = scalacOptions.value
        val classpath    = fullClasspath.value.map(_.data.toPath())
        val unmanaged    = Keys.unmanagedSources.value.map(_.toPath())
        val managed      = Keys.managedSources.value.map(_.toPath())
        val scala2Inputs = Scala2Inputs(projectId, scalaVersion, sOptions, classpath, unmanaged, managed)
        StateTransform(s => s.put(scala2inputsAttirbute, scala2Inputs))
      }
    )

  def prepareMigrateImpl = Def.task {
    val log = streams.value.log
    log.info(Messages.welcomePrepareMigration)
    val targetRoot = semanticdbTargetRoot.value
    val projectId  = thisProject.value.id
    // computed values
    val scala2InputsValue     = state.value.attributes.get(scala2inputsAttirbute).get
    val unamangedSources      = scala2InputsValue.unmanagedSources
    val scala2Classpath       = scala2InputsValue.classpath
    val scala2CompilerOptions = scala2InputsValue.scalacOptions

    Try {
      migrateAPI.prepareMigration(
        unamangedSources.asJava,
        targetRoot.toPath,
        scala2Classpath.asJava,
        scala2CompilerOptions.asJava
      )
    } match {
      case Success(_) =>
        log.info(Messages.successMessagePrepareMigration(projectId, scala3Version))
      case Failure(exception) =>
        log.err(Messages.errorMessagePrepareMigration(projectId, exception))
    }
  }

  def migrateImp =
    Def.task {
      val log = streams.value.log
      log.info(Messages.welcomeMigration)

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
          log.info(Messages.successOfMigration(thisProject.value.id, scala3Version))

        case Failure(exception) =>
          log.err(Messages.errorMesssageMigration(exception))
      }
    }

  private def sanitazeScala3Options(options: Seq[String]) = {
    val nonWorkingOptions = Set(syntheticsOn)
    options.filterNot(nonWorkingOptions)
  }
}
