lazy val `syntax-migration` = project
  .in(file("."))
  .settings(
    scalaVersion      := "3.8.1",
    semanticdbVersion := "4.14.2"
  )
