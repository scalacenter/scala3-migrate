import BuildInfoExtension._
import sbt.Keys.libraryDependencies

inThisBuild(
  List(
    scalaVersion               := V.scala213,
    semanticdbEnabled          := true,
    semanticdbVersion          := V.scalameta,
    scalafixScalaBinaryVersion := V.scala213BinaryVersion,
    organization               := "ch.epfl.scala",
    homepage                   := Some(url("https://github.com/scalacenter/scala3-migrate")),
    licenses                   := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers                 := Developers.list,
    version ~= { dynVer =>
      if (isCI) dynVer
      else V.localSnapshotVersion // only for local publishing
    }
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    publish / skip := true,
    // for scala-steward
    libraryDependencies += "org.scalameta" %% "scalameta" % V.scalameta
  )
  .aggregate(
    `migrate-interface`,
    `compiler-interface`,
    migrate,
    input,
    output,
    `sbt-plugin`,
    `scalafix-input`,
    `scalafix-output`,
    `scalafix-rules`,
    `scalafix-tests`
  )

lazy val `migrate-interface` = project
  .in(file("interfaces/migrate"))
  .settings(
    scalaVersion := V.scala3,
    libraryDependencies ++= Seq(
      "io.get-coursier" % "interface" % V.coursierInterface
    ),
    crossPaths       := false,
    autoScalaLibrary := false,
    moduleName       := "scala3-migrate-interface"
  )

lazy val `compiler-interface` = project
  .in(file("interfaces/compiler"))
  .settings(
    scalaVersion := V.scala3,
    libraryDependencies ++= Seq("org.scala-lang" %% "scala3-compiler" % V.scala3),
    crossPaths       := false,
    autoScalaLibrary := false,
    moduleName       := "scala3-migrate-compiler-interface"
  )
  .dependsOn(`migrate-interface`)

lazy val migrate = project
  .in(file("migrate"))
  .settings(addBuildInfoToConfig(Test))
  .settings(
    moduleName := "scala3-migrate-core",
    scalacOptions ++= Seq("-Wunused", "-P:semanticdb:synthetics:on", "-deprecation"),
    libraryDependencies ++= Seq(
      "org.scala-lang"   % "scala-compiler"      % scalaVersion.value,
      "ch.epfl.scala"    % "scalafix-interfaces" % V.scalafix,
      "io.get-coursier" %% "coursier"            % V.coursierApi,
      "org.scalatest"   %% "scalatest"           % V.scalatest % Test,
      "ch.epfl.scala"    % "scalafix-testkit"    % V.scalafix  % Test cross CrossVersion.full
    ),
    Test / test             := (Test / test).dependsOn(`scalafix-rules` / publishLocal).value,
    Test / testOnly         := (Test / testOnly).dependsOn(`scalafix-rules` / publishLocal).evaluated,
    Test / buildInfoPackage := "migrate.test",
    Test / buildInfoKeys := Seq(
      "version" -> version.value,
      "input"   -> (input / Compile / sourceDirectory).value,
      fromSources("unmanagedSources", input / Compile / unmanagedSources),
      fromSources("managedSources", input / Compile / managedSources),
      "output" -> (output / Compile / sourceDirectory).value,
      fromClasspath("scala2Classpath", input / Compile / fullClasspath),
      "semanticdbPath" -> (input / Compile / semanticdbTargetRoot).value,
      fromScalacOptions("scala2CompilerOptions", input / Compile / scalacOptions),
      fromClasspath("scala3Classpath", output / Compile / fullClasspath),
      fromScalacOptions("scala3CompilerOptions", output / Compile / scalacOptions),
      "scala3ClassDirectory" -> (output / Compile / compile / classDirectory).value,
      "baseDirectory"        -> (input / baseDirectory).value
    ),
    buildInfoPackage        := "migrate.buildinfo",
    Compile / buildInfoKeys := Seq("version" -> version.value, "scala3Version" -> V.scala3)
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(`migrate-interface`)
  .dependsOn(`compiler-interface`)

lazy val input = project
  .in(file("input"))
  .settings(
    publish / skip := true,
    scalacOptions ++= List("-P:semanticdb:synthetics:on"),
    libraryDependencies ++= List(
      "org.typelevel" %% "cats-core" % V.catsCore,
      compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full))
    )
  )
  .disablePlugins(ScalafixPlugin)

lazy val `sbt-plugin` = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    scalaVersion := V.scala212,
    name         := "sbt-scala3-migrate",
    libraryDependencies ++= Seq(
      "io.get-coursier" %% "coursier"                      % V.coursierApi,
      "io.get-coursier" %% "coursier-sbt-maven-repository" % V.coursierApi,
      "org.scalatest"   %% "scalatest"                     % V.scalatest % Test
    ),
    scriptedLaunchOpts ++= Seq(s"-Dplugin.version=${version.value}"),
    scriptedDependencies := scriptedDependencies
      .dependsOn(
        `migrate-interface` / publishLocal,
        `compiler-interface` / publishLocal,
        migrate / publishLocal,
        `scalafix-rules` / publishLocal
      )
      .value,
    buildInfoPackage  := "migrate",
    scriptedBufferLog := false,
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      "scala3Version"      -> V.scala3,
      "scalaBinaryVersion" -> V.scala213BinaryVersion,
      "scalametaVersion"   -> V.scalameta,
      version
    )
  )
  .dependsOn(`migrate-interface`)
  .disablePlugins(ScalafixPlugin)
  .enablePlugins(BuildInfoPlugin)

lazy val output = project
  .in(file("output"))
  .settings(
    publish / skip := true,
    scalaVersion   := V.scala3,
    scalacOptions  := Seq("-Ykind-projector"),
    libraryDependencies ++= Seq("org.typelevel" % "cats-core_2.13" % V.catsCore)
  )
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-rules` = project
  .in(file("scalafix/rules"))
  .settings(
    scalacOptions ++= List("-Wunused", "-P:semanticdb:synthetics:on"),
    moduleName := "scala3-migrate-rules",
    libraryDependencies ++= Seq(
      "ch.epfl.scala"  %% "scalafix-core"  % V.scalafix,
      ("ch.epfl.scala" %% "scalafix-rules" % V.scalafix).cross(CrossVersion.full)
    )
  )

lazy val `scalafix-input` = project
  .in(file("scalafix/input"))
  .settings(
    scalacOptions ++= List("-P:semanticdb:synthetics:on"),
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % V.catsCore,
      "dev.zio"       %% "zio"       % V.zio,
      compilerPlugin(("org.typelevel" %% "kind-projector" % V.kindProjector).cross(CrossVersion.full))
    ),
    buildInfoKeys := Seq[BuildInfoKey](name)
  )
  .enablePlugins(BuildInfoPlugin)
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-output` = project
  .in(file("scalafix/output"))
  .settings(
    publish / skip     := true,
    crossScalaVersions := List(V.scala213, V.scala3),
    scalacOptions ++= (if (scalaVersion.value.startsWith("3")) Seq("-Ykind-projector") else Seq()),
    libraryDependencies ++= {
      Seq("org.typelevel" %% "cats-core" % V.catsCore, "dev.zio" %% "zio" % V.zio) ++
        (CrossVersion.partialVersion(scalaVersion.value) match {
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
    publish / skip := true,
    scalacOptions ++= Seq("-Wunused", "-P:semanticdb:synthetics:on", "-deprecation"),
    libraryDependencies +=
      "ch.epfl.scala" %
        "scalafix-testkit" %
        V.scalafix %
        Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      (`scalafix-output` / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputSourceDirectories :=
      (`scalafix-input` / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputClasspath :=
      (`scalafix-input` / Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions :=
      (`scalafix-input` / Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      (`scalafix-input` / Compile / scalaVersion).value
  )
  .dependsOn(`scalafix-input`, `scalafix-rules`)
  .enablePlugins(ScalafixTestkitPlugin)

// for CI
addCommandAlias("compileScalafixOutputinScala3", s"""set `scalafix-output`/scalaVersion := "${V.scala3}" ; compile""")

def isCI = System.getenv("CI") != null

lazy val V = new {
  val scala213              = "2.13.15"
  val scala213BinaryVersion = "2.13"
  val scala212              = "2.12.20"
  val scalatest             = "3.2.19"
  val scala3                = "3.3.4"
  val scalafix              = "0.13.0"
  val catsCore              = "2.10.0"
  val kindProjector         = "0.13.3"
  val coursierApi           = "2.1.17"
  val coursierInterface     = "1.0.23"
  val scalameta             = "4.11.2"
  val localSnapshotVersion  = "0.7.0-SNAPSHOT"
  // scala-steward:off
  val zio = "1.0.18"
  // scala-steward:on
}
