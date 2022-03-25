lazy val `unresolved-dependencies` = project
  .in(file("."))
  .settings(
    scalaVersion := V.scala213,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % V.catsCore,
      compilerPlugin("org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full)
    ),
    TaskKey[Unit]("checkFallback") := {
      assert(scalaVersion.value == V.scala213, s"Wrong scala version ${scalaVersion.value}. Expected ${V.scala213}")
    }
  )

lazy val V = new {
  val scala213      = "2.13.8"
  val catsCore      = "2.7.0"
  val kindProjector = "0.13.2"
}
