import sbt.Keys.libraryDependencies

val interfaces = project.in(file("interfaces"))
  .settings(
    scalaVersion := V.scala213,
    libraryDependencies ++= Seq(
      "ch.epfl.lamp" % "dotty-compiler_0.27" % V.dotty,
    ),
    crossPaths := false,
    autoScalaLibrary := false
  )

val migrate = project.in(file("migrate"))
  .settings(
    scalaVersion := V.scala213,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "ch.epfl.scala" % "scalafix-interfaces" % V.scalafix,
      "io.get-coursier" %% "coursier" % V.coursier,
      "com.outr" %% "scribe" % V.scribe
    )
  )
  .dependsOn(interfaces)

val input = project.in(file("input"))
  .settings(
    scalaVersion := V.scala213
  )

val output = project.in(file("output"))
  .settings(
    scalaVersion := V.scala213
  )

val tests = project.in(file("tests"))
  .settings(
    scalaVersion := V.scala213,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % V.scalatest
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      "input" -> sourceDirectory.in(input, Compile).value,
      "output" -> sourceDirectory.in(output, Compile).value,
    )
  )
  .dependsOn(migrate)
  .enablePlugins(BuildInfoPlugin)

lazy val V = new {
  val scala213 = "2.13.3"
  val scalatest = "3.2.0"
  val dotty = "0.27.0-RC1"
  val scalafix = "0.9.20"
  val coursier = "2.0.0-RC6-25"
  val scribe = "2.7.12"
}
