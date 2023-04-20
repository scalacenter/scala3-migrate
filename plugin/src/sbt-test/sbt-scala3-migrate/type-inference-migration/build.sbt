lazy val `type-inference-migration` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.10",
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.2.2", s"Wrong scala version ${scalaVersion.value}. Expected 3.2.2")
      (Test / compile).value
    }
  )
