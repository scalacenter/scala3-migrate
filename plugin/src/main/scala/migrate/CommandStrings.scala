package migrate

object CommandStrings {
  val migrateSyntaxCommand = "migrateSyntax"
  val migrateSyntaxBrief   =
    (s"$migrateSyntaxCommand <projectId>", "Fix the syntax incompatibilities for Scala 3 in a projectId")
  val migrateSyntaxDetailed =
    s"""|Usage : $migrateSyntaxCommand <projectId>
        |
        |Fix the syntax incompatibilities for Scala 3 in the given project.
        |
        |""".stripMargin

  val migrateCommand = "migrateTypes"
  val migrateBrief   =
    (
      s"$migrateCommand <projectId>",
      "Migrate the project to Scala 3 by inferring necessary types and implicits"
    )
  val migrateDetailed =
    s"""|Usage : $migrateCommand <projectId>
        |
        |Add necessary types or implicits to make the project compile in Scala 3.
        |
        |""".stripMargin

  val migrateScalacOptionsCommand = "migrateScalacOptions"
  val migrateScalacOptionsBrief   =
    (s"$migrateScalacOptionsCommand <projectId>", "Report the migration status of each scalac option of a project.")
  val migrateScalacOptionsDetailed =
    s"""|Usage : $migrateScalacOptionsCommand <projectId>
        |
        |$migrateScalacOptionsBrief
        |The status of a scalac option can be:
        |- Valid: it can be used in Scala 3
        |- Renamed: it must be renamed
        |- Removed: it is no longer supported
        |- Unknown: sbt-scala3-migrate does not know this option
        |""".stripMargin

  val migrateLibs      = "migrateDependencies"
  val migrateLibsBrief = (
    s"$migrateLibs <projectId>",
    "Report the migration status of the library dependencies and compiler plugins of a project")
  val migrateLibsDetailed =
    s"""|Usage : $migrateLibs <projectId>
        |
        |$migrateLibsBrief
        |The status of a dependency can be:
        |- Valid: it can be used in Scala 3
        |- Updated version: the version needs to be updated
        |- For 3 use 2.13: it is compatible with Scala 3 using CrossVersion.for3Use2_13
        |- Incompatible: it is incompatible with Scala 3 because it is a macro library or a compiler plugin
        |- Unclassified: sbt-scala3-migrate does not know how to migrate this dependency
        |""".stripMargin
}
