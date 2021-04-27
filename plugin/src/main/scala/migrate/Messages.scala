package migrate

import migrate.interfaces.Lib
import scala.io.AnsiColor._

object Messages {

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
}
