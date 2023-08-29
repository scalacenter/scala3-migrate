package migrate

import Console._

private[migrate] object Messages {
  def notScala213(scalaVersion: String, projectId: String) =
    s"""
       |
       |Error:
       |
       |Your project must be in 2.13 and not in $scalaVersion
       |Please change the scalaVersion:
       |${YELLOW}scalaVersion := "2.13.11"${RESET}
       |
       |
       |""".stripMargin

  def computeLongestValue(values: Seq[String]): Int =
    if (values.isEmpty) 0 else values.maxBy(_.length).length

  def formatValueWithSpace(value: String, longestValue: Int): String = {
    val numberOfSpaces = " " * (longestValue - value.length)
    s"$value$numberOfSpaces"
  }
}
