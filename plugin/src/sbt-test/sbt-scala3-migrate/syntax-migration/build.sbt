lazy val `syntax-migration` = project
  .in(file("."))
  .settings(
    scalaVersion      := "3.10.0",
    semanticdbVersion := "4.14.2"
  )
