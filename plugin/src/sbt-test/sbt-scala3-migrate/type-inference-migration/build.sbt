lazy val `type-inference-migration` = project
  .in(file("."))
  .settings(
    scalaVersion      := "3.10.0",
    semanticdbVersion := "4.14.2"
  )
