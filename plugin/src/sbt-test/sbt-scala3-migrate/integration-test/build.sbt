lazy val `integration-test` = project
  .in(file("integration-test"))
  .configs(IntegrationTest)
  .settings(
    scalaVersion := "2.13.7",
    // Enable migration on IntegrationTest config
    inConfig(IntegrationTest)(Defaults.itSettings ++ ScalaMigratePlugin.configSettings),
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.1.0")
      (IntegrationTest / compile).value
    }
  )
