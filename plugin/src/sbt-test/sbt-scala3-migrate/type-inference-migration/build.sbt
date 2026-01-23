lazy val `type-inference-migration` = project
  .in(file("."))
  .settings(
    scalaVersion      := "3.8.1",
    semanticdbVersion := "4.14.2"
  )
