package migrate

object CommandStrings {

  val migrateSyntaxCommand = "migrate-syntax"
  val migrateSyntaxBrief =
    (s"$migrateSyntaxCommand <projectId>", "Fix syntax incompatibilities for scala 3 for a specific projectId")
  val migrateSyntaxDetailed =
    s"""|Usage : $migrateSyntaxCommand <projectId>
        |
        |Fix syntax incompatibilities of the projectId.
        |
        |""".stripMargin

  val migrateCommand = "migrate"
  val migrateBrief =
    (
      s"$migrateCommand <projectId>",
      "Migrate the project to scala 3 by inferring necessary types or implicits of a specific project"
    )
  val migrateDetailed =
    s"""|Usage : $migrateCommand <projectId>
        |
        |Add necessary types or implicits to make the projectId compiles in scala 3.
        |If the command succeeded, the project is compiling successfully in scala 3
        |
        |""".stripMargin

  val migrateScalacOptionsCommand = "migrate-scalacOptions"
  val migrateScalacOptionsBrief =
    (s"$migrateScalacOptionsCommand <projectId>", "Print scalacOptions that should be used for scala 3.")
  val migrateScalacOptionsDetailed =
    s"""|Usage : $migrateScalacOptionsCommand <projectId>
        |
        |Print the migrated scalacOptions for Scala 3 so you can update you scalacOptions
        | 
        |
        |""".stripMargin

  val migrateLibs      = "migrate-libs"
  val migrateLibsBrief = (s"$migrateLibs <projectId>", "Print the new versions of libs in order to migrate to Scala 3")
  val migrateLibsDetailed =
    s"""|Usage : $migrateLibs <projectId>
        |
        |Coursier is used to find, for each dependency, a Scala 3 version.
        |If the lib is not published for Scala 3 yet, versions compatible with 2.13 are reported, in case the lib does not contain macros.
        | 
        |
        |""".stripMargin
}
