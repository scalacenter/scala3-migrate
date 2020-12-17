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
}
