package migrate

import migrate.interfaces.InitialLibImp._
import migrate.interfaces.MigratedLib
import migrate.interfaces.MigratedLibImp
import migrate.internal.InitialLib
import migrate.internal.MigratedLib._
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class MigrateLibsSuite extends AnyFunSuiteLike with DiffAssertions {
  val binary: CrossVersion.Binary = CrossVersion.Binary("", "")

  val cats: InitialLib    = InitialLib.from("org.typelevel:cats-core:2.4.0", binary, None).get
  val cats213: InitialLib = InitialLib.from("org.typelevel:cats-core_2.13:2.4.0", CrossVersion.Disabled, None).get
  val opentelemetry: InitialLib =
    InitialLib.from("io.opentelemetry:opentelemetry-api:0.7.1", CrossVersion.Disabled, None).get
  val collection: InitialLib =
    InitialLib.from("org.scala-lang.modules:scala-collection-compat:2.4.0", binary, None).get
  val kind: InitialLib =
    InitialLib.from("org.typelevel:kind-projector:0.12.0", binary, Some("plugin->default(compile)")).get
  val scalafix: InitialLib = InitialLib.from("ch.epfl.scala:scalafix-core:0.9.24", binary, None).get
  val scalafix213: InitialLib =
    InitialLib.from("ch.epfl.scala:scalafix-core_2.13:0.9.24", CrossVersion.Disabled, None).get
  val macroLib: InitialLib = InitialLib.from("com.softwaremill.scalamacrodebug:macros:0.4.1", binary, None).get
  val javaLib2: InitialLib =
    InitialLib.from("org.eclipse.platform:org.eclipse.swt.win32.x86_64:3.116.0", CrossVersion.Disabled, None).get

  test("compiler plugin with a scalacOption available in Scala 3") {
    val migrated = Scala3Migrate.migrateLibs(Seq(kind)).allLibs
    val res      = migrated(kind)
    assert(res.isCompatibleWithScala3)
    assert(res.isInstanceOf[CompatibleWithScala3.ScalacOption])
  }
  test("Java lib") {
    val migrated = Scala3Migrate.migrateLibs(Seq(opentelemetry)).allLibs
    val res      = migrated(opentelemetry)
    assert(res.isCompatibleWithScala3)
    assert(res.getReasonWhy == MigratedLibImp.Reason.JavaLibrary.why)
    assert(isTheSame(opentelemetry, res))
  }

  test("java lib2") {
    val migrated = Scala3Migrate.migrateLibs(Seq(javaLib2)).allLibs
    val res      = migrated(javaLib2)
    assert(res.isCompatibleWithScala3)
    assert(isTheSame(javaLib2, res))
  }
  test("Available in scala 3") {
    val migrated = Scala3Migrate.migrateLibs(Seq(cats)).allLibs
    val res      = migrated(cats)
    assert(res.isCompatibleWithScala3)
    assert(res.asInstanceOf[CompatibleWithScala3.Lib].crossVersion.isInstanceOf[CrossVersion.For2_13Use3])
  }
  test("Don't show older version") {
    val migrated = Scala3Migrate.migrateLibs(Seq(collection)).allLibs
    val res      = migrated(collection).asInstanceOf[CompatibleWithScala3.Lib].revisions
    val revision = Revision("2.3.2")
    assert(!res.contains(revision))
  }
  test("Not available lib in scala 3 ") {
    val migrateLib = Scala3Migrate.migrateLibs(Seq(scalafix)).allLibs
    val res        = migrateLib(scalafix)
    assert(res.isCompatibleWithScala3)
    assert(!isTheSame(scalafix, res))
    assert(res.asInstanceOf[CompatibleWithScala3.Lib].crossVersion.isInstanceOf[CrossVersion.For3Use2_13])
  }
  // Warning: this test may change if the lib is ported to scala 3
  test("Not migrated because macro lib") {
    val migrateLib = Scala3Migrate.migrateLibs(Seq(macroLib)).allLibs
    val res        = migrateLib(macroLib)
    assert(!res.isCompatibleWithScala3)
  }

  private def isTheSame(lib: InitialLib, migrated: MigratedLib) =
    migrated match {
      case migrated: CompatibleWithScala3.Lib =>
        lib.organization == migrated.organization && lib.name == migrated.name && lib.revision == migrated.revision && lib.crossVersion == migrated.crossVersion
      case _ => false
    }
}
