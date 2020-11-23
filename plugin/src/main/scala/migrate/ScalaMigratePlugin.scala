package migrate

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import java.nio.file.{ Files, Path, Paths }

import migrate.interfaces.Migrate

import scala.collection.JavaConverters._
import buildinfo.BuildInfo

case class Scala3Inputs(scalacOptions: Seq[String], classpath: Seq[Path], classDirectory: Path)

object ScalaMigratePlugin extends AutoPlugin {
  val scala3inputsAttirbute = AttributeKey[Scala3Inputs]("scala3Inputs")
  val scalaBinaryVersion    = BuildInfo.scalaBinaryVersion
  val migrateVersion        = BuildInfo.version
  val toolClasspath         = BuildInfo.toolClasspath.split(java.io.File.pathSeparator).toList

  object autoImport {
    // val scala3CompilerOptions = taskKey[Seq[String]]("scalacOptions for scala 3")

    val scala3Version     = "0.27.0-RC1"
    val checkRequirements = taskKey[Unit]("check requirements")
    val migrate           = taskKey[Unit]("migrate a specific project to scala 3")
    val scala3Inputs      = taskKey[Scala3Inputs]("compute scala 3 classpath and options")
    val storeScala3Inputs = taskKey[StateTransform]("store scala 3 classpath and options in state attributes")
  }
  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def trigger = AllRequirements

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(configSettings) ++
      inConfig(Test)(configSettings)

  val configSettings: Seq[Setting[_]] =
    Seq(
      semanticdbEnabled := {
        if (scalaVersion.value.startsWith("2.13.")) true
        else semanticdbEnabled.value
      },
      migrate := migrateImp.value,
      scala3Inputs := {
        val state1 = Command.process(s"""set scalaVersion:="${scala3Version}" """, state.value)
        val state2 = Command.process("storeScala3Inputs", state1)
        (for {
          inputs <- state2.attributes.get(scala3inputsAttirbute)
        } yield Scala3Inputs(inputs.scalacOptions, inputs.classpath, inputs.classDirectory)).get

      },
      storeScala3Inputs := {
        val sOptions             = scalacOptions.value
        val classpath            = managedClasspath.value.seq.map(_.data.toPath())
        val scala3ClassDirectory = (compile / classDirectory).value.toPath
        val scala3Inputs         = Scala3Inputs(sOptions, classpath, scala3ClassDirectory)
        StateTransform(s => s.put(scala3inputsAttirbute, scala3Inputs))
      }
    )

  def migrateImp =
    Def.task {
      val log = streams.value.log
      log.info("we are going to migrate your project to scala 3 maybe")

      val sourcesPath           = sources.value.seq.map(_.toPath())
      val targetRoot            = semanticdbTargetRoot.value
      val scala2Classpath       = fullClasspath.value.seq.map(_.data.toPath())
      val scala2CompilerOptions = scalacOptions.value

      // computed values
      val scala3InputsValue = scala3Inputs.value
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
