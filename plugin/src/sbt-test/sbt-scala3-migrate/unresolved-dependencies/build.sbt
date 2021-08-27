lazy val `unresolved-dependencies` = project
  .in(file("."))
  .settings(
    scalaVersion := V.scala213,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % V.catsCore,
      compilerPlugin("org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full)
    ),
    TaskKey[Unit]("checkFallback") := {
      assert(scalaVersion.value == V.scala213)
    }
  )

lazy val V = new {
  val scala213      = "2.13.5"
  val catsCore      = "2.6.1"
  val kindProjector = "0.13.1"
}
