package migrate

object CommandStrings {

  val migratePrepareCommand = "migrate-prepare"
  val migratePrepareBrief   = (migratePrepareCommand, "Fix syntax incompatibilities for scala 3 for a specific projectId")
  val migratePreprareDetailed =
    s"""|$migratePrepareCommand <projectId>
        |
        |Fix syntax incompatibilities of the projectId.
        |
        |""".stripMargin

  val migrateCommand = "migrate"
  val migrateBrief =
    (migrateCommand, "Migrate the project to scala 3 by inferring necessary types or implicits of a specific project")
  val migrateDetailed =
    s"""|$migrateCommand <projectId>
        |
        |Add necessary types or implicits to make the projectId compiles in scala 3.
        |If the command succeeded, the project is compiling successfully in scala 3
        |
        |""".stripMargin

  val migrateScalacOptionsCommand = "migrate-scalacOptions"
  val migrateScalacOptionsBrief   = (migrateScalacOptionsCommand, "Print scalacOptions that should be used for scala 3")
  val migrateScalacOptionsDetailed =
    s"""|$migrateScalacOptionsCommand <projectId>
        |
        |Print the migrated scalacOptions for Scala 3 so you can update you scalacOptions
        | 
        |
        |""".stripMargin

  val migrateLibs      = "migrate-libs"
  val migrateLibsBrief = (migrateLibs, "Find and show the new versions of libs in order to migrate to Scala 3")
  val migrateLibsDetailed =
    s"""|$migrateLibs <projectId>
        |
        |Coursier is used to find, for each dependency, a Scala 3 version.
        |If the lib is not published for Scala 3 yet, versions compatible with 2.13 are reported, in case the lib does not contain macros.
        | 
        |
        |""".stripMargin
}
