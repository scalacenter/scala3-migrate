package migrate

import Console._

private[migrate] object Messages {
  def notScala213(scalaVersion: String, projectId: String): String =
    s"""|The current Scala Version of $projectId is $scalaVersion
        |Before migrating to Scala 3, please upgrade to the latest 2.13 version.
        |${YELLOW}scalaVersion := "2.13.11"${RESET}
        |""".stripMargin
}
