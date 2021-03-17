package migrate

import migrate.interfaces.Lib
import scala.io.AnsiColor._

object Messages {
  def welcomeMigration(projectD: String): String =
    s"${BOLD}We are going to migrate your project $projectD to ${ScalaMigratePlugin.scala3Version}${RESET}"
  def welcomeMigrateSyntax(projectD: String): String =
    s"${BOLD}We are going to fix some syntax incompatibilities on $projectD${RESET}"
  def notScala213(scalaVersion: String, projectId: String) =
    s"""
       |
       |Error:
       |
       |you project must be in 2.13 and not in $scalaVersion
       |please change the scalaVersion following this command
       |set LocalProject("$projectId") / scalaVersion := "2.13.5"
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

  def errorMesssageMigration(exceptionOpt: Option[Throwable]) = {
    val exceptionError = exceptionOpt.map(error => s"because of ${error.getMessage}").getOrElse("")
    s"""|
        |
        |Migration has failed
        |$exceptionError
        |
        |""".stripMargin
  }

  def successMessageMigrateSyntax(projectId: String, scala3: String) =
    s"""|
        |The syntax incompatibilities have been fixed on the project $projectId
        |You can now commit the change!
        |You can also execute the next command to try to migrate to $scala3
        |
        |migrate $projectId
        |
        |
        |""".stripMargin

  def errorMessageMigrateSyntax(projectId: String, ex: Throwable): String =
    s"""|
        |
        |Failed fixing the syntax for $projectId project
        |${ex.getMessage()}
        |
        |
        |""".stripMargin

  def migrationScalacOptionsStarting(projectId: String): String =
    s"""|
        |${BOLD}Starting to migrate the scalacOptions for $projectId${RESET}
        |""".stripMargin

  val warnMessageScalacOption: String =
    s"""|${YELLOW}Some scalacOptions are set by sbt plugins and don't need to be modified, removed or added.${RESET}
        |${YELLOW}The sbt plugin should adapt its own scalacOptions for Scala 3${RESET}""".stripMargin

  def notParsed(s: Seq[String]): Option[String] =
    if (s.nonEmpty)
      Some(s"""|
               |We were not able to parse the following ScalacOptions:
               |${formatScalacOptions(s)}
               |
               |""".stripMargin)
    else None

  def scalacOptionsMessage(
    removed: Seq[String],
    renamed: Map[String, String],
    scala3cOptions: Seq[String],
    pluginsOption: Seq[String]
  ): String = {
    val removedSign           = s"""${BOLD}${RED}X${RESET}"""
    val sameSign              = s"""${BOLD}${CYAN}Valid${RESET}"""
    val renamedSign           = s"""${BOLD}${BLUE}Renamed${RESET}"""
    def formatRemoved: String = removed.map(r => s""""$r" -> $removedSign""").mkString("\n")
    def formatRenamed: String = renamed.map { case (initial, renamed) =>
      s""""$initial" -> ${BOLD}${BLUE}"$renamed"${BLUE}"""
    }.mkString("\n")
    def formatScala3cOptions(s: Seq[String]): String = s.map(r => s""""$r" -> $sameSign""").mkString("\n")
    def pluginSettingsMessage: String =
      if (pluginsOption.isEmpty) ""
      else
        s"""|
            |${BOLD}The following scalacOption are specific to compiler plugins, usually added through `compilerPlugin` or `addCompilerPlugin`.${RESET}
            |In the previous step `migrate-libs`, you should have removed/fixed compiler plugins and for the remaining plugins and settings, they can be kept as they are.
            |
            |${formatScala3cOptions(pluginsOption)}
            |""".stripMargin
    s"""
       |$removedSign         $RED: The following scalacOption is specific to Scala 2 and doesn't have an equivalent in Scala 3$RESET
       |$renamedSign   $BLUE: The following scalacOption has been renamed in Scala3$RESET
       |$sameSign       $CYAN: The following scalacOption is a valid Scala 3 option$RESET
       |
       |$formatRemoved
       |$formatRenamed
       |${formatScala3cOptions(scala3cOptions)}
       |
       |""".stripMargin + pluginSettingsMessage
  }

  def migrateLibsStarting(projectId: String): String =
    s"""|
        |
        |${BOLD}Starting to migrate libDependencies for $projectId${RESET}
        |""".stripMargin

  def messageForLibs(
    notMigrated: Seq[Lib],
    validLibs: Seq[Lib],
    toUpdate: Map[Lib, Seq[Lib]],
    compilerPluginsWithScalacOption: Map[Lib, String]
  ): String = {
    val removedSign = s"""${BOLD}${RED}X${RESET}"""
    val validSign   = s"""${BOLD}${CYAN}Valid${RESET}"""
    val toBeUpdated = s"""${BOLD}${BLUE}To be updated${RESET}"""
    val commentMacro =
      s"${BOLD}${YELLOW}Contains Macros and is not yet published for ${ScalaMigratePlugin.scala3Version}${RESET}"
    val commentCompilerPlugin =
      s"${BOLD}${YELLOW}Compiler plugins are not supported in scala ${ScalaMigratePlugin.scala3Version}. You need to find an alternative${RESET}"
    val commentCompilerWithScalacOption =
      s"${BOLD}${YELLOW}This compiler plugin has a scalacOption equivalent. Add it to your scalacOptions$RESET"
    val notMigratedWithComments =
      notMigrated.map(lib => if (lib.isCompilerPlugin) (lib, commentCompilerPlugin) else (lib, commentMacro))
    def formatCompilerPlugins: String =
      compilerPluginsWithScalacOption.map { case (l, scalacOption) =>
        format(l, Seq(scalacOption)) + s" : $commentCompilerWithScalacOption"
      }.mkString("\n")
    def formatNotMigrated: String = notMigratedWithComments.map { case (lib, comment) =>
      s""""$lib" -> $removedSign : $comment"""
    }.mkString("\n")
    def formatValid: String = validLibs.map(lib => s""""$lib" -> $validSign""").mkString("\n")

    val help = s"""
                  |$removedSign             $RED: Cannot be updated to scala 3$RESET
                  |$validSign         $CYAN: Already a valid version for Scala 3$RESET
                  |$toBeUpdated $BLUE: Need to be updated to the following version$RESET
                  |""".stripMargin

    s"""|
        |$help
        |
        |$formatNotMigrated
        |$formatValid
        |${formatLibs(toUpdate)}
        |$formatCompilerPlugins
        |
        |""".stripMargin

  }

  private def formatLibs(libs: Map[Lib, Seq[Lib]]): String =
    libs.map { case (initial, migrated) => format(initial, migrated.map(_.toString)) }.mkString("\n")

  private def format(initial: Lib, migrated: Seq[String]): String =
    s"""\"$initial\" -> ${GREEN}${migrated.mkString(", ")}$RESET"""

  private def formatScalacOptions(l: Seq[String]): String =
    l.mkString("Seq(\n\"", "\",\n\"", "\"\n)")

}
