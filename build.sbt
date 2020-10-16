import BuildInfoExtension._

ThisBuild / scalaVersion := V.scala213

lazy val interfaces = project.in(file("interfaces"))
  .settings(
    libraryDependencies ++= Seq(
      "ch.epfl.lamp" % "dotty-compiler_0.27" % V.dotty,
    ),
    crossPaths := false,
    autoScalaLibrary := false
  )

lazy val migrate = project.in(file("migrate"))
  .settings(addBuildInfoToConfig(Test))
  .settings(
    scalacOptions ++= Seq(
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "ch.epfl.scala" % "scalafix-interfaces" % V.scalafix,
      "com.outr" %% "scribe" % V.scribe,
      "org.scalatest" %% "scalatest" % V.scalatest,
      "ch.epfl.scala" % "scalafix-testkit" % V.scalafix % Test cross CrossVersion.full,
    ),
    Test / buildInfoPackage := "migrate.test",
    Test / buildInfoKeys := Seq(
      "input" -> (input / Compile / sourceDirectory).value,
      "output" -> (output / Compile / sourceDirectory).value,
      "workspace" -> (ThisBuild / baseDirectory).value,
      fromClasspath("scala2Classpath", input / Compile / fullClasspath),
      "semanticdbPath" -> (input / Compile / semanticdbTargetRoot).value,
      fromScalacOptions("scala2CompilerOptions", input / Compile / scalacOptions),
      fromClasspath("toolClasspath", `scalafix-rules` / Compile / fullClasspath),
      fromClasspath("scala3Classpath", output / Compile / fullClasspath),
      fromScalacOptions("scala3CompilerOptions", output / Compile / scalacOptions),
      "scala3ClassDirectory" -> (output / Compile / compile / classDirectory).value,
    ),
    Compile / buildInfoKeys := Seq()
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(interfaces)

lazy val input = project.in(file("input"))
  .settings(
    semanticdbEnabled := true,
    scalacOptions ++= List("-P:semanticdb:synthetics:on")
  )

lazy val output = project.in(file("output"))
  .settings(
    scalaVersion := V.dotty,
  )

lazy val `scalafix-rules` = project.in(file("scalafix/rules"))
  .settings(
    moduleName := "scalafix",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %% "scalafix-core" % V.scalafix,
      "ch.epfl.scala" %% "scalafix-rules" % V.scalafix,
    )
  )

lazy val `scalafix-input` = project.in(file("scalafix/input"))
  .settings(
    semanticdbEnabled := true,
    scalacOptions ++= List(
      "-P:semanticdb:synthetics:on"
    ),
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.twitter" %% "bijection-core" % "0.9.7"
    )
  )

lazy val `scalafix-output` = project.in(file("scalafix/output"))
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.twitter" %% "bijection-core" % "0.9.7"
    )
  )


lazy val scalafix = project.in(file("scalafix/tests"))
  .settings(
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
  val scala213 = "2.13.3"
  val scalatest = "3.2.0"
  val dotty = "0.27.0-RC1"
  val scalafix = "0.9.20"
  val scribe = "2.7.12"
}