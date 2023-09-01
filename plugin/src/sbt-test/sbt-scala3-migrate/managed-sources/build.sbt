lazy val `managed-sources` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.11",
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "buildinfo" / "BuildInfo.scala"
      val buildInfo = s"""|
                          |package buildinfo
                          |
                          |object BuildInfo {
                          |  val scalaVersion = "${scalaVersion.value}"
                          |}
                          |""".stripMargin
      IO.write(file, buildInfo)
      Seq(file)
    }
  )
