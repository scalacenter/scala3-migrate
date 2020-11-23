import BuildInfoExtension._
import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion := V.scala213
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision // use Scalafix compatible version
ThisBuild / scalafixScalaBinaryVersion := V.scala213BinaryVersion
ThisBuild / scalafixDependencies ++= List("com.github.liancheng" %% "organize-imports" % V.organizeImports)
ThisBuild / organization := "ch.epfl.scala"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val `compiler-interfaces` = project
  .in(file("interfaces/compiler"))
  .settings(
    scalaVersion := V.dotty,
    libraryDependencies ++= Seq("ch.epfl.lamp" %% "dotty-compiler" % V.dotty),
    crossPaths := false,
    autoScalaLibrary := false
  )

lazy val `migrate-interfaces` = project
  .in(file("interfaces/migrate"))
  .settings(
    libraryDependencies ++= Seq("io.get-coursier" % "interface" % V.coursierInterface),
    crossPaths := false,
    autoScalaLibrary := false
  )

lazy val migrate = project
  .in(file("migrate"))
  .settings(addBuildInfoToConfig(Test))
  .settings(
    scalacOptions ++= Seq("-Wunused", "-P:semanticdb:synthetics:on", "-deprecation"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler"      % scalaVersion.value,
      "ch.epfl.scala"  % "scalafix-interfaces" % V.scalafix,
      "com.outr"      %% "scribe"              % V.scribe,
      "org.scalatest" %% "scalatest"           % V.scalatest % Test,
      "ch.epfl.scala"  % "scalafix-testkit"    % V.scalafix  % Test cross CrossVersion.full
    ),
    Test / buildInfoPackage := "migrate.test",
    Test / buildInfoKeys := Seq(
      "input" -> (input / Compile / sourceDirectory).value,
      fromSources("sources", (input / Compile / sources)),
      "output"    -> (output / Compile / sourceDirectory).value,
      "workspace" -> (ThisBuild / baseDirectory).value,
      fromClasspath("scala2Classpath", input / Compile / fullClasspath),
      "semanticdbPath" -> (input / Compile / semanticdbTargetRoot).value,
      fromScalacOptions("scala2CompilerOptions", input / Compile / scalacOptions),
      fromClasspath("toolClasspath", `scalafix-rules` / Compile / fullClasspath),
      fromClasspath("scala3Classpath", output / Compile / fullClasspath),
      fromScalacOptions("scala3CompilerOptions", output / Compile / scalacOptions),
      "scala3ClassDirectory" -> (output / Compile / compile / classDirectory).value
    ),
    Compile / buildInfoKeys := Seq()
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(`compiler-interfaces`)
  .dependsOn(`migrate-interfaces`)

lazy val input = project
  .in(file("input"))
  .settings(
    scalacOptions ++= List("-P:semanticdb:synthetics:on"),
    libraryDependencies ++= List(
      "org.typelevel"                 %% "cats-core"      % V.catsCore,
      compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full))
    )
  )
  .disablePlugins(ScalafixPlugin)

lazy val `sbt-plugin` = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    scalaVersion := V.scala212,
    name := "sbt-scala-migrat3",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      "scalaBinaryVersion" -> V.scala213BinaryVersion,
      version,
      fromClasspath("toolClasspath", `scalafix-rules` / Compile / fullClasspath)
    )
  )
  .dependsOn(`migrate-interfaces`)
  .disablePlugins(ScalafixPlugin)
  .enablePlugins(BuildInfoPlugin)

lazy val output = project
  .in(file("output"))
  .settings(
    scalaVersion := V.dotty,
    scalacOptions := Seq("-Ykind-projector"),
    libraryDependencies ++= Seq("org.typelevel" % "cats-core_2.13" % V.catsCore)
  )
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-rules` = project
  .in(file("scalafix/rules"))
  .settings(
    scalacOptions ++= List("-Wunused", "-P:semanticdb:synthetics:on"),
    moduleName := "scalafix",
    libraryDependencies ++= Seq("ch.epfl.scala" %% "scalafix-core" % V.scalafix)
  )

lazy val `scalafix-input` = project
  .in(file("scalafix/input"))
  .settings(
    scalacOptions ++= List("-P:semanticdb:synthetics:on"),
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.scala-lang"                 % "scala-reflect"  % scalaVersion.value,
      "com.twitter"                   %% "bijection-core" % V.bijectionCore,
      "org.typelevel"                 %% "cats-core"      % V.catsCore,
      compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full))
    )
  )
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-output` = project
  .in(file("scalafix/output"))
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.scala-lang"                 % "scala-reflect"  % scalaVersion.value,
      "com.twitter"                   %% "bijection-core" % V.bijectionCore,
      "org.typelevel"                 %% "cats-core"      % V.catsCore,
      compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full))
    )
  )
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-tests` = project
  .in(file("scalafix/tests"))
  .settings(
    scalacOptions ++= Seq("-Wunused", "-P:semanticdb:synthetics:on", "-deprecation"),
    libraryDependencies +=
      "ch.epfl.scala" %
        "scalafix-testkit" %
        V.scalafix %
        Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      sourceDirectories.in(`scalafix-output`, Compile).value,
    scalafixTestkitInputSourceDirectories :=
      sourceDirectories.in(`scalafix-input`, Compile).value,
    scalafixTestkitInputClasspath :=
      fullClasspath.in(`scalafix-input`, Compile).value,
    scalafixTestkitInputScalacOptions :=
      scalacOptions.in(`scalafix-input`, Compile).value,
    scalafixTestkitInputScalaVersion :=
      scalaVersion.in(`scalafix-input`, Compile).value
  )
  .dependsOn(`scalafix-input`, `scalafix-rules`)
  .enablePlugins(ScalafixTestkitPlugin)

lazy val V = new {
  val scala213              = "2.13.3"
  val scala213BinaryVersion = "2.13"
  val scala212              = "2.12.11"
  val scalatest             = "3.2.3"
  val dotty                 = "0.27.0-RC1"
  val scalafix              = "0.9.23"
  val scribe                = "3.0.4"
  val organizeImports       = "0.4.3"
  val bijectionCore         = "0.9.7"
  val catsCore              = "2.2.0"
  val kindProjector         = "0.11.0"
  val coursierInterface     = "0.0.25"
}
