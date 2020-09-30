inThisBuild(
  List(
    scalaVersion := V.scala213,
  )
)

val interfaces = project.in(file("interfaces"))
  .settings(
    libraryDependencies ++= Seq(
      "ch.epfl.lamp" % "dotty-compiler_0.27" % V.dotty,
    ),
    crossPaths := false,
    autoScalaLibrary := false
  )

val migrate = project.in(file("migrate"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "ch.epfl.scala" % "scalafix-interfaces" % V.scalafix,
      "io.get-coursier" %% "coursier" % V.coursier,
      "com.outr" %% "scribe" % V.scribe
    )
  )
  .dependsOn(interfaces, `scalafix-rules`)

val input = project.in(file("input"))
  .settings(
    semanticdbEnabled := true
  )

val output = project.in(file("output"))
  .settings(
    scalaVersion := V.dotty
  )

val tests = project.in(file("tests"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % V.scalatest
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      "input" -> sourceDirectory.in(input, Compile).value,
      "output" -> sourceDirectory.in(output, Compile).value,
    ),
    Compile / resourceGenerators += Def.task {
      val props = new java.util.Properties()
      props.put("migrateClasspath", fullClasspath.in(`scalafix-rules`, Compile)
        .value.map(_.data).mkString(java.io.File.pathSeparator))
      val out = managedResourceDirectories.in(Compile).value.head / "migrate.properties"
      IO.write(props, "Input data for migrate tests", out)
      List(out)
    }
  )
  .dependsOn(migrate)
  .enablePlugins(BuildInfoPlugin)

lazy val `scalafix-rules` = project.in(file("scalafix/rules"))
  .settings(
  moduleName := "scalafix",
  libraryDependencies ++= Seq(
    "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion,
    "ch.epfl.scala" %% "scalafix-rules" % V.scalafixVersion,
  )
)

lazy val `scalafix-input` = project.in(file("scalafix/input"))
  .settings(
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


lazy val `scalafix-tests` = project.in(file("scalafix/tests"))
  .settings(
    libraryDependencies +=
      "ch.epfl.scala" %
        "scalafix-testkit" %
        V.scalafixVersion %
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
  val coursier = "2.0.0-RC6-25"
  val scribe = "2.7.12"
  val scalafixVersion = "0.9.20"
}
