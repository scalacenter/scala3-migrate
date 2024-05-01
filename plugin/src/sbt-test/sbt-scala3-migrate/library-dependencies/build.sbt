lazy val `library-dependencies` = project
  .in(file("."))
  .settings(
    scalaVersion := V.scala213,
    libraryDependencies +=
      ("org.typelevel" %% "cats-core" % V.catsCore).cross(CrossVersion.for3Use2_13),
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("3")) Seq("-Ykind-projector")
      else Seq.empty
    },
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("3")) Seq.empty
      else Seq(compilerPlugin("org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full))
    }
  )

lazy val V = new {
  val scala213      = "2.13.11"
  val catsCore      = "2.10.0"
  val kindProjector = "0.13.3"
}
