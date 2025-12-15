import migrate.ScalaMigratePlugin.Keys._

ThisBuild / scalaVersion := "2.13.18"

ThisBuild / semanticdbVersion := "4.14.2"

lazy val subproject = project
  .in(file("subproject"))
  .settings(
    Compile / internalMigrateDependencies := {
      throw new Exception("subproject / Compile / internalMigrateDependencies")
    },
    Compile / internalMigrateScalacOptions := {
      throw new Exception("subproject / Compile / internalMigrateScalacOptions")
    },
    Compile / internalMigrateSyntax := {
      throw new Exception("subproject / Compile / internalMigrateSyntax")
    },
    Compile / internalMigrateTypes := {
      throw new Exception("subproject / Compile / internalMigrateTypes")
    }
  )

lazy val `aggregate-project` = project
  .in(file("."))
  .aggregate(subproject)
