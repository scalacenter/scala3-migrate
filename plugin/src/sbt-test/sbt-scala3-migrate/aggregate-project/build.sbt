ThisBuild / scalaVersion := "2.13.11"

lazy val subproject = project
  .in(file("subproject"))
  .settings(TaskKey[Unit]("checkNotMigrated") := {
    assert(scalaVersion.value == "2.13.11")
  })

lazy val `aggregate-project` = project
  .in(file("."))
  .settings(TaskKey[Unit]("checkMigration") := {
    assert(scalaVersion.value == "3.3.0", s"Wrong scala version ${scalaVersion.value}. Expected 3.3.0")
    (Compile / compile).value
  })
  .aggregate(subproject)
