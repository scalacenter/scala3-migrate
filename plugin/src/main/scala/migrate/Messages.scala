package migrate

object Messages {
  def welcomeMigration: String        = "We are going to migrate your project to scala 3"
  def welcomePrepareMigration: String = "We are going to fix some syntax incompatibilities"
  def notScala213(scalaVersion: String, projectId: String) =
    s"""
       |
       |Error:
       |
       |you project must be in 2.13 and not in $scalaVersion
       |please change the scalaVersion following this command
       |set $projectId / scalaVersion := "2.13.3"
       |
       |
       |""".stripMargin

  def successOfMigration(projectId: String, scala3: String): String =
    s"""|
        |
        |$projectId project has successfully been migrated to scala $scala3
        |You can now commit the change!
        |You can also execute the compile command:
        |
        |$projectId / compile
        |
        |
        |""".stripMargin

  def errorMesssageMigration(ex: Throwable) =
    s"""|
        |
        |Migration has failed!
        |${ex.getMessage}
        |
        |
        |""".stripMargin

  def successMessagePrepareMigration(projectId: String, scala3: String) =
    s"""|
        |
        |We fixed the syntax of this $projectId to be compatible with $scala3
        |You can now commit the change!
        |You can also execute the next command to try to migrate to $scala3
        |
        |migrate projectId
        |
        |
        |""".stripMargin

  def errorMessagePrepareMigration(projectId: String, ex: Throwable) =
    s"""|
        |
        |Failed fixing the syntax for $projectId project
        |${ex.getMessage()}
        |
        |
        |""".stripMargin
}
