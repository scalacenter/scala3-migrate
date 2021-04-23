lazy val `syntax-migration` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.5",
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.0.0-RC2")
      (Compile / compile).value
    }
  )
