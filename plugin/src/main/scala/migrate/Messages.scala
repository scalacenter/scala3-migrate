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

  def errorMesssageMigration() =
    s"""|
        |
        |Migration has failed!
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
        |migrate $projectId
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

  def migrationScalacOptionsStarting(projectId: String) =
    s"""|
        |
        |Starting to migrate ScalacOptions for $projectId
        |""".stripMargin

  def notParsed(s: Seq[String]): Option[String] =
    if (s.nonEmpty)
      Some(s"""|
               |We were not able to parse the following ScalacOptions:
               |${format(s)}
               |
               |""".stripMargin)
    else None

  def specificToScala2(s: Seq[String]): Option[String] =
    if (s.nonEmpty)
      Some(s"""|
               |The Following scalacOptions are specific to scala 2 and don't have an equivalent in Scala 3
               |${format(s)}
               |
               |""".stripMargin)
    else None

  def migrated(s: Seq[String]): String =
    if (s.nonEmpty)
      s"""|
          |You can update your scalacOptions for Scala 3 with the following settings.
          |${format(s)}
          |
          |
          |
          |""".stripMargin
    else
      """|
         |The ScalacOptions for Scala 3 is empty. 
         |
         |
         |
         |""".stripMargin

  private def format(l: Seq[String]): String =
    l.mkString("Seq(\n\"", "\",\n\"", "\"\n)")

}
