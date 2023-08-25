package migrate

import migrate.internal.ScalacOption
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrateScalacOptionsSuite extends AnyFunSuiteLike with DiffAssertions {
  test("sanitize options") {
    val input = Seq(
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:existentials",
      "-Xlint:_,-type-parameter-shadow",
      "-Xfatal-warnings",
      "-Wunused patvars"
    )
    val expected = Seq(
      "-encoding UTF-8",
      "-language:higherKinds",
      "-language:existentials",
      "-Xlint:_,-type-parameter-shadow",
      "-Xfatal-warnings",
      "-Wunused patvars"
    )
    val sanitized = ScalacOption.sanitize(input)
    assert(sanitized == expected)
  }

  test("Empty ScalacOptions list") {
    val input = Nil
    val res   = Scala3Migrate.migrateScalacOptions(input)
    assert(res.notParsed.isEmpty)
    assert(res.specificScala2.isEmpty)
    assert(res.scala3cOptions.isEmpty)
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
      "-Wunused",
      "patvars"
    )
    val res               = Scala3Migrate.migrateScalacOptions(input)
    val expectedSpecific2 = Seq("-Yrangepos", "-Xlint:_,-type-parameter-shadow")
    val expectedMigrated = Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:higherKinds",
      "-language:existentials",
      "-unchecked",
      "-deprecation",
      "-Werror",
      "-Wunused",
      "patvars",
      "-explain",
      "-source:2.13"
    )
    assert(res.notParsed.isEmpty, "All scalacOptions should be parsed correctly")
    assert(res.specificScala2.map(_.scala2Value) == expectedSpecific2)
    assert((res.scala3cOptions ++ res.renamed).flatMap(_.scala3ValueForBuild) == expectedMigrated)
  }
  test("scalacOption -target") {
    val input = Seq("-target:jvm-1.8")
    val res   = Scala3Migrate.migrateScalacOptions(input).renamed
    assert(res.head.scala3Value == "-Xunchecked-java-output-version:8")
  }
}
