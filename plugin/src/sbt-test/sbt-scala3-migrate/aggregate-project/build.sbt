ThisBuild / scalaVersion := "2.13.5"

lazy val subproject = project
  .in(file("subproject"))
  .settings(TaskKey[Unit]("checkNotMigrated") := {
    assert(scalaVersion.value == "2.13.5")
  })

lazy val `aggregate-project` = project
  .in(file("."))
  .settings(TaskKey[Unit]("checkMigration") := {
    assert(scalaVersion.value == "3.0.0")
    (Compile / compile).value
  })
  .aggregate(subproject)
