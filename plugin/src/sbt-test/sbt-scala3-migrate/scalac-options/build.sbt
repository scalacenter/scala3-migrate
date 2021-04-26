import migrate.ScalaMigratePlugin

lazy val `scalac-options` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.5",
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
