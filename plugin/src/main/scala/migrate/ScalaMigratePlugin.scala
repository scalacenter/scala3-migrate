package migrate

import java.nio.file.{ Files, Path, Paths }

import buildinfo.BuildInfo
import migrate.interfaces.Migrate
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.collection.JavaConverters._

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
  sources: Seq[Path]
)

object ScalaMigratePlugin extends AutoPlugin {
  val scala3inputsAttirbute = AttributeKey[Scala3Inputs]("scala3Inputs")
  val scala2inputsAttirbute = AttributeKey[Scala2Inputs]("scala2Inputs")
  val scalaBinaryVersion    = BuildInfo.scalaBinaryVersion
  val migrateVersion        = BuildInfo.version
  val toolClasspath         = BuildInfo.toolClasspath.split(java.io.File.pathSeparator).toList
  val scala3Version         = BuildInfo.scala3Version

  object autoImport {
    val checkRequirements = taskKey[Unit]("check requirements")

    val scala3InputComputed = taskKey[Scala3Inputs]("show scala 3 inputs if available")
    val scala2InputComputed = taskKey[Scala2Inputs]("show scala 2 inputs if available")

    val storeScala3Inputs = taskKey[StateTransform]("store scala 3 inputs")
    val storeScala2Inputs = taskKey[StateTransform]("store scala 2 inputs")
    val internalMigrate   = taskKey[Unit]("migrate a specific project to scala 3")
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def trigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(configSettings) ++
      inConfig(Test)(configSettings) ++
      Seq(commands ++= Seq(migrateCommand))

  lazy val migrateCommand: Command =
    Command.single("migrate") { (state, projectId) =>
      import sbt.BasicCommandStrings._
      val result = List(
        StashOnFailure,
        s"$projectId / storeScala2Inputs",
        s"""set $projectId / scalaVersion := "${scala3Version}"""",
        s"$projectId / storeScala3Inputs",
        s"$projectId / internalMigrate",
        PopOnFailure,
        FailureWall
      ) ::: state
      result
    }

  val configSettings: Seq[Setting[_]] =
    Seq(
      semanticdbEnabled := {
        if (scalaVersion.value.startsWith("2.13.")) true
        else semanticdbEnabled.value
      },
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
          inputs.sources
        )).get
      },
      storeScala3Inputs := {
        val projectId            = thisProject.value.id
        val scalaVersion         = Keys.scalaVersion.value
        val sOptions             = scalacOptions.value
        val classpath            = managedClasspath.value.seq.map(_.data.toPath())
        val scala3ClassDirectory = (compile / classDirectory).value.toPath
        val scala3Inputs         = Scala3Inputs(projectId, scalaVersion, sOptions, classpath, scala3ClassDirectory)
        StateTransform(s => s.put(scala3inputsAttirbute, scala3Inputs))
      },
      storeScala2Inputs := {
        val projectId    = thisProject.value.id
        val scalaVersion = Keys.scalaVersion.value
        val sOptions     = scalacOptions.value
        val classpath    = managedClasspath.value.seq.map(_.data.toPath())
        val sources      = Keys.sources.value.seq.map(_.toPath())
        val scala2Inputs = Scala2Inputs(projectId, scalaVersion, sOptions, classpath, sources)
        StateTransform(s => s.put(scala2inputsAttirbute, scala2Inputs))
      }
    )

  def migrateImp =
    Def.task {
      val log = streams.value.log
      log.info("we are going to migrate your project to scala 3 maybe")

      val targetRoot = semanticdbTargetRoot.value

      // computed values
      val scala2InputsValue     = state.value.attributes.get(scala2inputsAttirbute).get
      val scala2Classpath       = scala2InputsValue.classpath
      val scala2CompilerOptions = scala2InputsValue.scalacOptions
      val sourcesPath           = scala2InputsValue.sources

      val scala3InputsValue = state.value.attributes.get(scala3inputsAttirbute).get
      val scalac3Options    = scala3InputsValue.scalacOptions
      val scala3Classpath   = scala3InputsValue.classpath
      val scala3ClassDir    = scala3InputsValue.classDirectory
      if (!Files.exists(scala3ClassDir)) Files.createDirectory(scala3ClassDir)

      // value from buildInfo
      val toolCp = toolClasspath.map(Paths.get(_))

      val migrateAPI = Migrate.fetchAndClassloadInstance(migrateVersion, scalaBinaryVersion)

      migrateAPI.migrate(
        sourcesPath.asJava,
        targetRoot.toPath(),
        scala2Classpath.asJava,
        scala2CompilerOptions.asJava,
        toolCp.asJava,
        scala3Classpath.asJava,
        scalac3Options.asJava,
        scala3ClassDir
      )
    }
}
