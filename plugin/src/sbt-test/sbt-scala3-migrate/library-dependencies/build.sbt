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
    },
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.0.0")
      (Compile / compile).value
    }
  )

lazy val V = new {
  val scala213      = "2.13.5"
  val catsCore      = "2.6.1"
  val kindProjector = "0.13.0"
}
