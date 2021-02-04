package migrate

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrateScalacOptionsSuite extends AnyFunSuiteLike with DiffAssertions {
  test("Empty ScalacOptions list") {
    val input = Nil
    val res   = ScalaMigrat.migrateScalacOptions(input)
    assert(res.notParsed.isEmpty)
    assert(res.specificScala2.isEmpty)
    assert(res.migrated.isEmpty)
  }
  test("ScalacOptions 1") {
    val input = Seq(
      "-encoding",
      "UTF-8",
      "-explaintypes",
      "-Yrangepos",
      "-feature",
      "-language:higherKinds",
      "-language:existentials",
      "-Xlint:_,-type-parameter-shadow",
      "-Xsource:2.13",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-Wunused:patvars"
    )
    val res               = ScalaMigrat.migrateScalacOptions(input)
    val expectedSpecific2 = Seq("-Yrangepos", "-Xlint:_,-type-parameter-shadow", "-Wunused:patvars")
    val expectedMigrated = Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:higherKinds",
      "-language:existentials",
      "-unchecked",
      "-deprecation",
      "-explain-types",
      "-source:2.13",
      "-Xfatal-warnings"
    )
    assert(res.notParsed.isEmpty, "All scalacOptions should be parsed correctly")
    assert(res.specificScala2.map(_.scala2Value) == expectedSpecific2)
    assert(res.migrated.flatMap(_.scala3ValueForBuild) == expectedMigrated)
  }
}
