package migrate

import scala.Console._

import migrate.internal.CrossCompatibleLibrary
import migrate.internal.CrossVersion
import migrate.internal.IncompatibleLibrary
import migrate.internal.InitialLib
import migrate.internal.UpdatedVersion
import migrate.internal.ValidLibrary
import org.scalatest.funsuite.AnyFunSuiteLike

class LibraryMigrationSuite extends AnyFunSuiteLike {
  val binaryJvm: CrossVersion.Binary = CrossVersion.Binary("", "")
  val binaryJs: CrossVersion.Binary  = CrossVersion.Binary("sjs1_", "")
  val fullJvm: CrossVersion.Full     = CrossVersion.Full("", "")
  val pluginConfig: Some[String]     = Some("plugin->default(compile)")

  val cats: InitialLib             = InitialLib("org.typelevel:cats-core:2.4.0", binaryJvm)
  val cats213: InitialLib          = InitialLib("org.typelevel:cats-core_2.13:2.4.0", CrossVersion.Disabled)
  val opentelemetry: InitialLib    = InitialLib("io.opentelemetry:opentelemetry-api:0.7.1", CrossVersion.Disabled)
  val collectionCompat: InitialLib = InitialLib("org.scala-lang.modules:scala-collection-compat:2.4.0", binaryJvm)
  val scalafix: InitialLib         = InitialLib("ch.epfl.scala:scalafix-core:0.9.24", binaryJvm)
  val javaLib2: InitialLib =
    InitialLib("org.eclipse.platform:org.eclipse.swt.win32.x86_64:3.116.0", CrossVersion.Disabled)
  val macroLib: InitialLib         = InitialLib("com.softwaremill.scalamacrodebug:macros:0.4.1", binaryJvm)
  val kindProjector: InitialLib    = InitialLib("org.typelevel:kind-projector:0.12.0", fullJvm, pluginConfig)
  val betterMonadicFor: InitialLib = InitialLib("com.olegpy:better-monadic-for:0.3.1", binaryJvm, pluginConfig)

  val scalatestJS: InitialLib = InitialLib("org.scalatest:scalatest:3.2.8", binaryJs)
  val domtypes: InitialLib    = InitialLib("com.raquo:domtypes:0.14.3", binaryJs)
  val domutils: InitialLib    = InitialLib("com.raquo:domtestutils:0.14.7", binaryJs)

  test("Integrated compiler plugin: kind projector") {
    val migrated  = LibraryMigration.migrateLib(kindProjector)
    val formatted = migrated.formatted
    val expected =
      s"""addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.12.0").cross(CrossVersion.full))""" +
        "\n" +
        s"""replaced by ${YELLOW}scalacOptions += "-Ykind-projector"$RESET"""
    assert(formatted == expected)
  }

  test("Incompatible compiler plugin: better monadic for") {
    val migrated  = LibraryMigration.migrateLib(betterMonadicFor)
    val formatted = migrated.formatted
    val expected  = s"""addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1") $RED(Compiler Plugin)$RESET"""
    assert(formatted == expected)
  }

  test("Java lib") {
    val migrated = LibraryMigration.migrateLib(opentelemetry)
    val expected = ValidLibrary(opentelemetry)
    assert(migrated == expected)
  }

  test("Java lib 2") {
    val migrated = LibraryMigration.migrateLib(javaLib2)
    val expected = ValidLibrary(javaLib2)
    assert(migrated == expected)
  }

  test("Available in scala 3") {
    val migrated = LibraryMigration.migrateLib(cats)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val version = migrated.asInstanceOf[UpdatedVersion].versions
    assert(version.head.toString == "2.6.1")
  }

  test("CrossVersion.Disabled to CrossVersion.Binary") {
    val migrated = LibraryMigration.migrateLib(cats213)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val updatedVersion = migrated.asInstanceOf[UpdatedVersion]
    assert(updatedVersion.versions.head.toString == "2.6.1")
    assert(updatedVersion.lib == cats)
  }
  test("Don't show older version") {
    val migrated = LibraryMigration.migrateLib(collectionCompat)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val updatedVersions = migrated.asInstanceOf[UpdatedVersion].versions
    assert(!updatedVersions.contains("2.3.2"))
  }
  test("Cross compatible lib") {
    val migrated = LibraryMigration.migrateLib(scalafix)
    val expected = CrossCompatibleLibrary(scalafix)
    assert(migrated == expected)
  }
  // Warning: this test may change if the lib is ported to scala 3
  test("Incompatible because macro lib") {
    val migrated = LibraryMigration.migrateLib(macroLib)
    val expected = IncompatibleLibrary(macroLib, "Macro Library")
    assert(migrated == expected)
  }

  test("Filtered out libs") {
    val scalaLib     = InitialLib("org.scala-lang:scala-library:2.13.14", CrossVersion.Disabled)
    val scalajs      = InitialLib("org.scala-js:scalajs-compiler:1.5.0", CrossVersion.Disabled)
    val migratedLibs = LibraryMigration.migrateLibs(Seq(scalaLib, scalajs))
    assert(migratedLibs.getValidLibraries.isEmpty)
    assert(migratedLibs.getUpdatedVersions.isEmpty)
    assert(migratedLibs.getCrossCompatibleLibraries.isEmpty)
    assert(migratedLibs.getIntegratedPlugins.isEmpty)
    assert(migratedLibs.getIncompatibleLibraries.isEmpty)
  }

  test("Formatting of updated versions") {
    val updatedVersions = UpdatedVersion(collectionCompat, Seq("2.4.4", "2.5.0", "2.6.0"))
    val formatted       = updatedVersions.formatted
    val expected =
      s""""org.scala-lang.modules" %% "scala-collection-compat" % "${YELLOW}2.4.4$RESET" $YELLOW(Other versions: 2.5.0, 2.6.0)$RESET"""
    assert(formatted == expected)
  }

  test("Formatting of valid Scala.js library") {
    val migratedLib = LibraryMigration.migrateLib(domtypes)
    val formatted   = migratedLib.formatted
    val expected    = s""""com.raquo" %%% "domtypes" % "0.14.3""""
    assert(formatted == expected)
  }

  test("Formatting of Scala.js updated version 1") {
    val updatedVersions = UpdatedVersion(scalatestJS, Seq("3.2.9", "3.2.10"))
    val formatted       = updatedVersions.formatted
    val expected =
      s""""org.scalatest" %%% "scalatest" % "${YELLOW}3.2.9$RESET" ${YELLOW}(Other version: 3.2.10)$RESET"""
    assert(formatted == expected)
  }

  test("Formatting of Scala.js updated version 2") {
    val updatedVersions = UpdatedVersion(domutils, Seq("0.14.8"))
    val formatted       = updatedVersions.formatted
    val expected        = s""""com.raquo" %%% "domtestutils" % "${YELLOW}0.14.8$RESET""""
    assert(formatted == expected)
  }

  test("Formatting of cross compatible library") {
    val crossCompatibleLibrary = CrossCompatibleLibrary(domtypes)
    val formatted              = crossCompatibleLibrary.formatted
    val expected               = s"""("com.raquo" %%% "domtypes" % "0.14.3")$YELLOW.cross(CrossVersion.for3Use2_13)$RESET"""
    assert(formatted == expected)
  }
}
