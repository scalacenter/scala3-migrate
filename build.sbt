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
    scalaVersion := V.scala3,
    libraryDependencies ++= Seq("org.scala-lang" %% "scala3-compiler" % V.scala3),
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
      "org.scala-lang"   % "scala-compiler"      % scalaVersion.value,
      "ch.epfl.scala"    % "scalafix-interfaces" % V.scalafix,
      "com.outr"        %% "scribe"              % V.scribe,
      "io.get-coursier" %% "coursier"            % V.coursierApi,
      "org.scalatest"   %% "scalatest"           % V.scalatest % Test,
      "ch.epfl.scala"    % "scalafix-testkit"    % V.scalafix  % Test cross CrossVersion.full
    ),
    Test / buildInfoPackage := "migrate.test",
    Test / buildInfoKeys := Seq(
      "input" -> (input / Compile / sourceDirectory).value,
      fromSources("unmanagedSources", input / Compile / unmanagedSources),
      fromSources("managedSources", input / Compile / managedSources),
      "output" -> (output / Compile / sourceDirectory).value,
      fromClasspath("scala2Classpath", input / Compile / fullClasspath),
      "semanticdbPath" -> (input / Compile / semanticdbTargetRoot).value,
      fromScalacOptions("scala2CompilerOptions", input / Compile / scalacOptions),
      fromClasspath("toolClasspath", `scalafix-rules` / Compile / fullClasspath),
      fromClasspath("scala3Classpath", output / Compile / fullClasspath),
      fromScalacOptions("scala3CompilerOptions", output / Compile / scalacOptions),
      "scala3ClassDirectory" -> (output / Compile / compile / classDirectory).value
    ),
    Compile / buildInfoKeys := Seq(fromClasspath("toolClasspath", `scalafix-rules` / Compile / fullClasspath))
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
    name := "sbt-scala3-migrate",
    scriptedLaunchOpts ++= Seq(s"-Dplugin.version=${version.value}"),
    scriptedDependencies := {
      scriptedDependencies
        .dependsOn(publishLocal in `migrate-interfaces`, publishLocal in `compiler-interfaces`, publishLocal in migrate)
        .value
    },
    buildInfoPackage := "migrate",
    scriptedBufferLog := false,
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      "scala3Version"      -> V.scala3,
      "scalaBinaryVersion" -> V.scala213BinaryVersion,
      version
    )
  )
  .dependsOn(`migrate-interfaces`)
  .disablePlugins(ScalafixPlugin)
  .enablePlugins(BuildInfoPlugin)

lazy val output = project
  .in(file("output"))
  .settings(
    scalaVersion := V.scala3,
    scalacOptions := Seq("-Ykind-projector"),
    libraryDependencies ++= Seq("org.typelevel" % "cats-core_2.13" % V.catsCore)
  )
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-rules` = project
  .in(file("scalafix/rules"))
  .settings(
    scalacOptions ++= List("-Wunused", "-P:semanticdb:synthetics:on"),
    moduleName := "scalafix",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %% "scalafix-core"  % V.scalafix,
      "ch.epfl.scala" %% "scalafix-rules" % V.scalafix
    )
  )

lazy val `scalafix-input` = project
  .in(file("scalafix/input"))
  .settings(
    scalacOptions ++= List("-P:semanticdb:synthetics:on"),
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.typelevel"                 %% "cats-core"      % V.catsCore,
      compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full))
    ),
    buildInfoKeys := Seq[BuildInfoKey](name)
  )
  .enablePlugins(BuildInfoPlugin)
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-output` = project
  .in(file("scalafix/output"))
  .settings(
    skip in publish := true,
    crossScalaVersions := List(V.scala213, V.scala3),
    scalacOptions ++= (if (ScalaArtifacts.isScala3(scalaVersion.value)) Seq("-Ykind-projector") else Seq()),
    libraryDependencies ++= {
      Seq(("org.typelevel" % "cats-core_2.13" % V.catsCore)) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) =>
          Seq(compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full)))
        case Some((3, _)) => Seq()
        case _            => Seq()
      })
    },
    buildInfoKeys := Seq[BuildInfoKey](name)
  )
  .enablePlugins(BuildInfoPlugin)
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
      unmanagedSourceDirectories.in(`scalafix-output`, Compile).value,
    scalafixTestkitInputSourceDirectories :=
      unmanagedSourceDirectories.in(`scalafix-input`, Compile).value,
    scalafixTestkitInputClasspath :=
      fullClasspath.in(`scalafix-input`, Compile).value,
    scalafixTestkitInputScalacOptions :=
      scalacOptions.in(`scalafix-input`, Compile).value,
    scalafixTestkitInputScalaVersion :=
      scalaVersion.in(`scalafix-input`, Compile).value
  )
  .dependsOn(`scalafix-input`, `scalafix-rules`)
  .enablePlugins(ScalafixTestkitPlugin)

// for CI
addCommandAlias("compileScalafixOutputinScala3", s"""set `scalafix-output`/scalaVersion := "${V.scala3}" ; compile""")

lazy val V = new {
  val scala213              = "2.13.4"
  val scala213BinaryVersion = "2.13"
  val scala212              = "2.12.11"
  val scalatest             = "3.2.3"
  val scala3                = "3.0.0-M3"
  val scalafix              = "0.9.25"
  val scribe                = "3.3.1"
  val organizeImports       = "0.4.3"
  val catsCore              = "2.3.1"
  val kindProjector         = "0.11.3"
  val coursierInterface     = "1.0.2"
  val coursierApi           = "2.0.11"
}
