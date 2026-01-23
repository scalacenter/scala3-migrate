import migrate.ScalaMigratePlugin

lazy val `scalac-options-migration` = project
  .in(file("."))
  .settings(
    scalaVersion      := "3.8.1",
    semanticdbVersion := "4.14.2",
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:implicitConversions",
      "-deprecation",
      "-Xfatal-warnings",
      "-Wunused:imports,privates,locals",
      "-Wvalue-discard"
    )
  )
