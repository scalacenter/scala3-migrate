lazy val `integration-test` = project
  .in(file("integration-test"))
  .configs(IntegrationTest)
  .settings(
    scalaVersion      := "3.8.1",
    semanticdbVersion := "4.14.2",
    // Enable migration on IntegrationTest config
    inConfig(IntegrationTest)(Defaults.itSettings ++ ScalaMigratePlugin.configSettings)
  )
