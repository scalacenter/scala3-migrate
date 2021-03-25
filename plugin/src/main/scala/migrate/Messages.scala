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
       |Your project must be in 2.13 and not in $scalaVersion
       |Please change the scalaVersion following this command
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
    val exceptionError = exceptionOpt
      .map(error => s"""|because of ${error.getMessage}
                        |${error.getStackTrace.mkString("\n")}""".stripMargin)
      .getOrElse("")
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
        |${ex.getStackTrace.mkString("\n")}
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
    val removedSign       = s"""${BOLD}${RED}X${RESET}"""
    val sameSign          = s"""${BOLD}${CYAN}Valid${RESET}"""
    val renamedSign       = s"""${BOLD}${BLUE}Renamed${RESET}"""
    val spacesHelp        = computeLongestValue(Seq(removedSign, sameSign, renamedSign))
    val spaceScalacOption = computeLongestValue(removed ++ renamed.keys ++ scala3cOptions)
    def formatRemoved(longest: Int): String =
      removed.map(r => s"""${formatValueWithSpace(r, longest)} -> $removedSign""").mkString("\n")
    def formatRenamed(longest: Int): String = renamed.map { case (initial, renamed) =>
      s"""${formatValueWithSpace(initial, longest)} -> ${BOLD}${BLUE}$renamed${BLUE}"""
    }.mkString("\n")
    def formatScala3cOptions(s: Seq[String], spaces: Int): String =
      s.map(r => s"""${formatValueWithSpace(r, spaces)} -> $sameSign""").mkString("\n")
    def pluginSettingsMessage: String =
      if (pluginsOption.isEmpty) ""
      else {
        val longestValue = computeLongestValue(pluginsOption)
        s"""|
            |${BOLD}The following scalacOption are specific to compiler plugins, usually added through `compilerPlugin` or `addCompilerPlugin`.${RESET}
            |In the previous step `migrate-libs`, you should have removed/fixed compiler plugins and for the remaining plugins and settings, they can be kept as they are.
            |
            |${formatScala3cOptions(pluginsOption, longestValue)}
            |""".stripMargin
      }

    val help = s"""
                  |${formatValueWithSpace(removedSign, spacesHelp)} $RED: The following scalacOption is specific to Scala 2 and doesn't have an equivalent in Scala 3$RESET
                  |${formatValueWithSpace(renamedSign, spacesHelp)} $BLUE: The following scalacOption has been renamed in Scala3$RESET
                  |${formatValueWithSpace(sameSign, spacesHelp)} $CYAN: The following scalacOption is a valid Scala 3 option$RESET
                  |""".stripMargin

    Seq(
      help,
      formatRemoved(spaceScalacOption),
      formatRenamed(spaceScalacOption),
      formatScala3cOptions(scala3cOptions, spaceScalacOption),
      pluginSettingsMessage
    ).filterNot(_.isEmpty).mkString("\n")
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
      s"${BOLD}${YELLOW}Scala 2 compiler plugins are not supported in scala ${ScalaMigratePlugin.scala3Version}. You need to find an alternative${RESET}"
    val commentCompilerWithScalacOption =
      s"${BOLD}${YELLOW}This compiler plugin has a scalacOption equivalent. Add it to your scalacOptions$RESET"

    val spacesForLib = computeLongestValue(
      (notMigrated ++ validLibs ++ toUpdate.keys ++ compilerPluginsWithScalacOption.keys).map(_.toString)
    )

    val notMigratedWithComments =
      notMigrated.map(lib => if (lib.isCompilerPlugin) (lib, commentCompilerPlugin) else (lib, commentMacro))
    def formatCompilerPlugins: String =
      compilerPluginsWithScalacOption.map { case (l, scalacOption) =>
        format(l, Seq(scalacOption), spacesForLib) + s" : $commentCompilerWithScalacOption"
      }.mkString("\n")
    def formatNotMigrated: String = notMigratedWithComments.map { case (lib, comment) =>
      s"""${formatValueWithSpace(lib.toString, spacesForLib)} -> $removedSign : $comment"""
    }.mkString("\n")
    def formatValid: String =
      validLibs.map(lib => s"""${formatValueWithSpace(lib.toString, spacesForLib)} -> $validSign""").mkString("\n")

    val spacesForHelp = computeLongestValue(Seq(removedSign, validSign, toBeUpdated))

    val help = s"""
                  |${formatValueWithSpace(removedSign, spacesForHelp)} $RED: Cannot be updated to scala 3$RESET
                  |${formatValueWithSpace(validSign, spacesForHelp)} $CYAN: Already a valid version for Scala 3$RESET
                  |${formatValueWithSpace(toBeUpdated, spacesForHelp)} $BLUE: Need to be updated to the following version$RESET
                  |""".stripMargin

    Seq(help, formatNotMigrated, formatValid, formatLibs(toUpdate, spacesForLib), formatCompilerPlugins)
      .filterNot(_.isEmpty)
      .mkString("\n")
  }

  def computeLongestValue(values: Seq[String]): Int =
    if (values.isEmpty) 0 else values.maxBy(_.length).length

  def formatValueWithSpace(value: String, longestValue: Int): String = {
    val numberOfSpaces = " " * (longestValue - value.length)
    s"$value$numberOfSpaces"
  }

  private def formatLibs(libs: Map[Lib, Seq[Lib]], longestValue: Int): String =
    libs.map { case (initial, migrated) => format(initial, migrated.map(_.toString), longestValue) }.mkString("\n")

  private def format(initial: Lib, migrated: Seq[String], longestValue: Int): String = {
    val numberOfSpaces = " " * (longestValue - initial.toString.length)
    s"""$initial$numberOfSpaces -> ${GREEN}${migrated.mkString(", ")}$RESET"""
  }

  private def formatScalacOptions(l: Seq[String]): String =
    l.mkString("Seq(\n\"", "\",\n\"", "\"\n)")

}
