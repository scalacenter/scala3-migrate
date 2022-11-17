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
      assert(scalaVersion.value == "3.1.1", s"Wrong scala version ${scalaVersion.value}. Expected 3.1.1")
      (Compile / compile).value
    }
  )

lazy val V = new {
  val scala213      = "2.13.8"
  val catsCore      = "2.9.0"
  val kindProjector = "0.13.2"
}
