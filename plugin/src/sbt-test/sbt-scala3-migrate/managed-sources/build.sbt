lazy val `managed-sources` = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.7",
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
    },
    TaskKey[Unit]("checkMigration") := {
      assert(scalaVersion.value == "3.1.0", s"Wrong scala version ${scalaVersion.value}. Expected 3.1.0")
      (Compile / compile).value
    }
  )
