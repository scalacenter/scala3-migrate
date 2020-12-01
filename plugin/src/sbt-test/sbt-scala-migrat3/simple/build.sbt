ThisBuild / scalaVersion := V.scala213
ThisBuild / semanticdbEnabled := true

lazy val main = project
  .in(file("main"))
  .settings(
    name := "hello-world",
    scalacOptions ++= (if (isDotty.value) Seq("-Ykind-projector") else Seq()),
    libraryDependencies ++= (
      if (isDotty.value) Seq.empty
      else
        Seq(compilerPlugin("org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full))
    ),
    libraryDependencies ++= Seq(
      ("org.typelevel" %% "cats-core" % V.catsCore)
        .withDottyCompat(scalaVersion.value)
    )
  )

lazy val basic = project
  .in(file("basic"))
  .settings(
    libraryDependencies ++=
      Seq(
        ("org.typelevel" %% "cats-core" % V.catsCore),
        compilerPlugin("org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full)
      )
  )

lazy val V = new {
  val scala213      = "2.13.3"
  val catsCore      = "2.3.0"
  val kindProjector = "0.11.0"
}
