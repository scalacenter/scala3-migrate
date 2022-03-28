lazy val `type-inference-migration` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.8",
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.1.1", s"Wrong scala version ${scalaVersion.value}. Expected 3.1.1")
      (Test / compile).value
    }
  )
