ThisBuild / scalaVersion := "2.13.8"

lazy val subproject = project
  .in(file("subproject"))
  .settings(TaskKey[Unit]("checkNotMigrated") := {
    assert(scalaVersion.value == "2.13.8")
  })

lazy val `aggregate-project` = project
  .in(file("."))
  .settings(TaskKey[Unit]("checkMigration") := {
    assert(scalaVersion.value == "3.1.1", s"Wrong scala version ${scalaVersion.value}. Expected 3.1.1")
    (Compile / compile).value
  })
  .aggregate(subproject)
