package migrate

import migrate.interfaces.Lib

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
       |set $projectId / scalaVersion := "2.13.5"
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
               |${formatScalacOptions(s)}
               |
               |""".stripMargin)
    else None

  def specificToScala2(s: Seq[String]): Option[String] =
    if (s.nonEmpty)
      Some(s"""|
               |The Following scalacOptions are specific to scala 2 and don't have an equivalent in Scala 3
               |${formatScalacOptions(s)}
               |
               |""".stripMargin)
    else None

  def migrated(s: Seq[String]): String =
    if (s.nonEmpty)
      s"""|
          |You can update your scalacOptions for Scala 3 with the following settings.
          |${formatScalacOptions(s)}
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

  def migrateLibsStarting(projectId: String): String =
    s"""|
        |
        |Starting to migrate libDependencies for $projectId
        |""".stripMargin

  def notMigratedLibs(libs: Seq[Lib]): String = {
    val (compilerPlugins, others) = libs.partition(_.isCompilerPlugin)
    val messageCompilerPlugin = if (compilerPlugins.nonEmpty) {
      s"""|
          |The following compiler plugins are not supported in scala 3
          |${compilerPlugins.map(_.toString).mkString("\n")}
          |""".stripMargin
    } else ""
    val messageOtherLibs =
      if (others.nonEmpty)
        s"""
           |The following list of libs cannot be migrated.
           |Please check the migration guide for more information. 
           |${others.map(_.toString).mkString("\n")}
           |
           |""".stripMargin
      else ""
    messageCompilerPlugin + messageOtherLibs
  }

  def migratedLib(libs: Map[Lib, Seq[Lib]]): String =
    s"""|
        |
        |You can update your libs with the following versions: 
        |
        |${format(libs)}
        |
        |
        |
        |""".stripMargin

  private def format(libs: Map[Lib, Seq[Lib]]): String =
    libs.map(format).mkString("\n")

  private def format(l: (Lib, Seq[Lib])): String = {
    val initial  = l._1
    val migrated = l._2
    s"""\"$initial\" -> ${migrated.mkString(", ")}"""
  }

  private def formatScalacOptions(l: Seq[String]): String =
    l.mkString("Seq(\n\"", "\",\n\"", "\"\n)")

}
