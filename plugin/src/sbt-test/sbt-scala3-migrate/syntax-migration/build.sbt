lazy val `syntax-migration` = project
  .in(file("."))
  .settings(
    scalaVersion      := "2.13.18",
    semanticdbVersion := "4.14.2"
  )
