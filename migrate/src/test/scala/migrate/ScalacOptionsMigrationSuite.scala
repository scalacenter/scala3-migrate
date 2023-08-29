package migrate

import scala.jdk.CollectionConverters._

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class ScalacOpionsMigrationSuite extends AnyFunSuiteLike with DiffAssertions {
  test("sanitize scalacOptions") {
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
    val sanitized = ScalacOptionsMigration.sanitize(input)
    assert(sanitized == expected)
  }
  test("migrate scalacOptions") {
    val input = Seq(
      "-encoding",
      "UTF-8",
      "-explaintypes",
      "-Yrangepos",
      "-feature",
      "-language:higherKinds",
      "--language:existentials",
      "-Xlint:_,-type-parameter-shadow",
      "-Xsource:2.13",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-Wunused",
      "patvars",
      "-target:jvm-1.8",
      "-unknown"
    )
    val res             = ScalacOptionsMigration.migrate(input)
    val expectedRemoved = Seq("-Yrangepos", "-Xlint:_,-type-parameter-shadow", "-Xsource:2.13")
    val expectedValid = Seq(
      "-encoding UTF-8",
      "-feature",
      "-language:higherKinds",
      "--language:existentials",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-Wunused patvars"
    )
    val expectedRenamed = Map(
      "-explaintypes"   -> "-explain",
      "-target:jvm-1.8" -> s"-Xunchecked-java-output-version:8"
    )
    val expectedUnknown = Seq("-unknown")

    assert(res.getValid.toSeq == expectedValid)
    assert(res.getRenamed.asScala == expectedRenamed)
    assert(res.getRemoved.toSeq == expectedRemoved)
    assert(res.getUnknown.toSeq == expectedUnknown)
  }
  test("parse target") {
    assert(ScalacOptionsMigration.migrateJavaTarget(" jvm-1.8") == " 8")
    assert(ScalacOptionsMigration.migrateJavaTarget(":jvm-1.9") == ":9")
    assert(ScalacOptionsMigration.migrateJavaTarget(" jvm-11") == " 11")
    assert(ScalacOptionsMigration.migrateJavaTarget(":jvm-17") == ":17")
    assert(ScalacOptionsMigration.migrateJavaTarget(" 1.8") == " 8")
    assert(ScalacOptionsMigration.migrateJavaTarget(":1.21") == ":21")
  }
}
