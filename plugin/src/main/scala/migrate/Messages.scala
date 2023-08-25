package migrate

private[migrate] object Messages {
  def notScala213(scalaVersion: String, projectId: String) =
    s"""
       |
       |Error:
       |
       |Your project must be in 2.13 and not in $scalaVersion
       |Please change the scalaVersion following this command
       |set LocalProject("$projectId") / scalaVersion := "2.13.11"
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
