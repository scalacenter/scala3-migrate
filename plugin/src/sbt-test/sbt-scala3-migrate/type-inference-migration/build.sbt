lazy val `type-inference-migration` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.5",
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.0.2", s"Wrong scala version ${scalaVersion.value}. Expected 3.0.2")
      (Test / compile).value
    }
  )
