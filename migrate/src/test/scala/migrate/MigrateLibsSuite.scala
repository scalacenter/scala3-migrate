package migrate

import migrate.LibToMigrate.CrossVersion
import migrate.LibToMigrate.Revision
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrateLibsSuite extends AnyFunSuiteLike with DiffAssertions {
  val binary: CrossVersion.Binary = CrossVersion.Binary("", "")

  val zioTests: Lib213      = Lib213.from("dev.zio:zio-test:1.0.4", binary).get
  val opentelemetry: Lib213 = Lib213.from("io.opentelemetry:opentelemetry-api:0.7.1", CrossVersion.Disabled).get
  val collection: Lib213    = Lib213.from("org.scala-lang.modules:scala-collection-compat:2.4.0", binary).get
  val kind: Lib213          = Lib213.from("org.typelevel:kind-projector:0.11.3", binary).get
  val scalafix: Lib213      = Lib213.from("ch.epfl.scala:scalafix-core:0.9.24", binary).get
  val macroLib: Lib213      = Lib213.from("com.softwaremill.scalamacrodebug:macros:0.4.1", binary).get

  test("Not available lib") {
    val migrated = ScalaMigrat.migrateLibs(Seq(kind))
    assert(migrated(kind).isEmpty)
  }
  test("Java lib") {
    val migrated = ScalaMigrat.migrateLibs(Seq(opentelemetry))
    assert(migrated(opentelemetry).size == 1)
    val res = migrated(opentelemetry).head
    assert(isTheSame(opentelemetry, res))
  }
  test("Available in scala 3") {
    val migrated = ScalaMigrat.migrateLibs(Seq(zioTests))
    val res      = migrated(zioTests)
    assert(res.nonEmpty)
    println(s"res.map(_.crossVersion) = ${res.map(_.crossVersion)}")
    assert(res.forall(_.crossVersion.isInstanceOf[CrossVersion.For2_13Use3]))
  }
  test("Don't show older version") {
    val migrated = ScalaMigrat.migrateLibs(Seq(collection))
    val res      = migrated(collection).map(_.revision)
    val revision = Revision("2.3.2")
    assert(!res.contains(revision))
  }
  test("Not available lib in scala 3 ") {
    val migrateLib = ScalaMigrat.migrateLibs(Seq(scalafix))
    val res        = migrateLib(scalafix)
    assert(res.nonEmpty)
    assert(res.forall(_.crossVersion.isInstanceOf[CrossVersion.For3Use2_13]))
    assert(res.map(_.revision).contains(scalafix.revision))
  }
  // Warning: this test may change if the lib is ported to scala 3
  test("Not migrated because macro lib") {
    val migrateLib = ScalaMigrat.migrateLibs(Seq(macroLib))
    val res        = migrateLib(macroLib)
    assert(res.isEmpty)
  }

  private def isTheSame(lib: Lib213, migrated: CompatibleWithScala3Lib) =
    lib.organization == migrated.organization && lib.name == migrated.name && lib.revision == migrated.revision && lib.crossVersion == migrated.crossVersion
}
