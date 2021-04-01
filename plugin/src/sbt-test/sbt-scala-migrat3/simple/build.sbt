ThisBuild / scalaVersion := V.scala213
ThisBuild / semanticdbEnabled := true

lazy val main = project
  .in(file("main"))
  .settings(
    name := "hello-world",
    scalacOptions ++= (if (ScalaArtifacts.isScala3(scalaVersion.value)) Seq("-Ykind-projector") else Seq()),
    libraryDependencies ++= (
      if (ScalaArtifacts.isScala3(scalaVersion.value)) Seq.empty
      else
        Seq(compilerPlugin("org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full))
    ),
    libraryDependencies ++= Seq(("org.typelevel" % "cats-core_2.13" % V.catsCore)),
    buildInfoObject := "Simple",
    buildInfoPackage := "simple",
    buildInfoKeys := Seq[BuildInfoKey](name, "scalaVersion" -> V.scala213)
  )
  .enablePlugins(BuildInfoPlugin)

lazy val basic = project
  .in(file("basic"))
  .settings(
    libraryDependencies ++=
      Seq(
        ("org.typelevel" %% "cats-core" % V.catsCore),
        compilerPlugin("org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full)
      )
  )

lazy val V = new {
  val scala213      = "2.13.5"
  val catsCore      = "2.5.0"
  val kindProjector = "0.11.3"
}
