package migrate

import migrate.internal.CompatibleWithScala3Lib
import migrate.internal.LibToMigrate
import migrate.internal.LibToMigrate.CrossVersion
import migrate.internal.LibToMigrate.Revision
import migrate.internal.ScalacOption
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrateLibsSuite extends AnyFunSuiteLike with DiffAssertions {
  val binary: CrossVersion.Binary = CrossVersion.Binary("", "")

  val cats: Lib213          = LibToMigrate.from("org.typelevel:cats-core:2.4.0", binary, None).get
  val cats213: Lib213       = LibToMigrate.from("org.typelevel:cats-core_2.13:2.4.0", CrossVersion.Disabled, None).get
  val opentelemetry: Lib213 = LibToMigrate.from("io.opentelemetry:opentelemetry-api:0.7.1", CrossVersion.Disabled, None).get
  val collection: Lib213    = LibToMigrate.from("org.scala-lang.modules:scala-collection-compat:2.4.0", binary, None).get
  val kind: Lib213          = LibToMigrate.from("org.typelevel:kind-projector:0.11.3", binary, Some("plugin->default(compile)")).get
  val scalafix: Lib213      = LibToMigrate.from("ch.epfl.scala:scalafix-core:0.9.24", binary, None).get
  val scalafix213: Lib213   = LibToMigrate.from("ch.epfl.scala:scalafix-core_2.13:0.9.24", CrossVersion.Disabled, None).get
  val macroLib: Lib213      = LibToMigrate.from("com.softwaremill.scalamacrodebug:macros:0.4.1", binary, None).get

  test("Not available lib") {
    val res      = Scala3Migrate.migrateLibs(Seq(kind))
    val compiler = res.compilerPlugins
    assert(compiler.nonEmpty)
    assert(compiler.values.head.get == ScalacOption.Specific3.KindProjector)
  }
  test("Java lib") {
    val migrated = Scala3Migrate.migrateLibs(Seq(opentelemetry)).libs
    assert(migrated(opentelemetry).size == 1)
    val res = migrated(opentelemetry).head
    assert(isTheSame(opentelemetry, res))
  }
  test("Available in scala 3") {
    val migrated = Scala3Migrate.migrateLibs(Seq(cats)).libs
    val res      = migrated(cats)
    assert(res.nonEmpty)
    assert(res.forall(_.crossVersion.isInstanceOf[CrossVersion.For2_13Use3]))
  }
  test("Don't show older version") {
    val migrated = Scala3Migrate.migrateLibs(Seq(collection)).libs
    val res      = migrated(collection).map(_.revision)
    val revision = Revision("2.3.2")
    assert(!res.contains(revision))
  }
  test("Not available lib in scala 3 ") {
    val migrateLib = Scala3Migrate.migrateLibs(Seq(scalafix)).libs
    val res        = migrateLib(scalafix)
    assert(res.nonEmpty)
    assert(res.forall(_.crossVersion.isInstanceOf[CrossVersion.For3Use2_13]))
    assert(res.map(_.revision).contains(scalafix.revision))
  }
  // Warning: this test may change if the lib is ported to scala 3
  test("Not migrated because macro lib") {
    val migrateLib = Scala3Migrate.migrateLibs(Seq(macroLib)).libs
    val res        = migrateLib(macroLib)
    assert(res.isEmpty)
  }

  private def isTheSame(lib: Lib213, migrated: CompatibleWithScala3Lib) =
    lib.organization == migrated.organization && lib.name == migrated.name && lib.revision == migrated.revision && lib.crossVersion == migrated.crossVersion
}
