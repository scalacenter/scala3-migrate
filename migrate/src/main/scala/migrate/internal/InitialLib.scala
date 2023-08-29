package migrate.internal

import scala.jdk.OptionConverters._

import migrate.interfaces.Lib

case class InitialLib(
  organization: String,
  name: String,
  version: String,
  crossVersion: CrossVersion,
  configurations: Option[String]
) {
  def isCompilerPlugin: Boolean = configurations.contains("plugin->default(compile)")
}

object InitialLib {
  def apply(lib: Lib): InitialLib = {
    val crossVersion = CrossVersion(lib.getCrossVersion)
    InitialLib(lib.getOrganization, lib.getName, lib.getVersion, crossVersion, lib.getConfigurations.toScala)
  }

  def apply(module: String, crossVersion: CrossVersion, configurations: Option[String] = None): InitialLib = {
    val splited = module.split(":").toList
    splited match {
      case (org :: name :: version :: Nil) =>
        InitialLib(org, name, version, crossVersion, configurations)
      case _ => throw new IllegalArgumentException(module)
    }
  }

  val macroLibraries: Set[(String, String)] =
    // need to complete the list
    // the other solution would be to download the src-jar and look for =\w*macro\w
    Set(
      ("com.softwaremill.scalamacrodebug", "macros"),
      ("com.github.ajozwik", "macro"),
      ("eu.timepit", "refined"),
      ("org.backuity", "ansi-interpolator"),
      ("com.github.dmytromitin", "auxify-macros"),
      ("biz.enef", "slogging"),
      ("io.getquill", "quill-jdbc"),
      ("com.lihaoyi", "fastparse"),
      ("com.github.kmizu", "macro_peg"),
      ("com.michaelpollmeier", "macros"),
      ("me.lyh", "parquet-avro-extra"),
      ("org.spire-math", "imp"),
      ("com.github.plokhotnyuk.expression-evaluator", "expression-evaluator"),
      ("com.github.plokhotnyuk.fsi", "fsi-macros"),
      ("com.wix", "accord-api"),
      ("org.typelevel", "claimant"),
      ("com.typesafe.slick", "slick"),
      ("com.github.pureconfig", "pureconfig"),
      ("com.geirsson", "metaconfig-typesafe-config"),
      ("com.thoughtworks.each", "each"),
      ("dev.zio", "zio-macros-core"),
      ("com.michaelpollmeier", "macros")
    )

  // Those libs are correctly handled by sbt, scalajs plugin or scoverage plugin
  // Showing them would confuse the user.
  val migrationFilter: Set[(String, String)] =
    Set(
      ("org.scala-js", "scalajs-compiler"),
      ("org.scala-js", "scalajs-library"),
      ("org.scala-js", "scalajs-test-bridge"),
      ("org.scala-lang", "scala-reflect"),
      ("org.scala-lang", "scala-library"),
      ("org.scoverage", "scalac-scoverage-plugin"),
      ("org.scalameta", "semanticdb-scalac")
    )
}
