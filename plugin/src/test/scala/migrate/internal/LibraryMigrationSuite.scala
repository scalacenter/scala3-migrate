package migrate.internal

import scala.Console._

import coursier.Repositories
import org.scalatest.funsuite.AnyFunSuiteLike
import sbt.librarymanagement.CrossVersion
import coursier.core.Repository
import coursier.maven.MavenRepository

class LibraryMigrationSuite extends AnyFunSuiteLike {
  val binaryJvm: CrossVersion.Binary = CrossVersion.Binary("", "")
  val binaryJs: CrossVersion.Binary  = CrossVersion.Binary("sjs1_", "")
  val fullJvm: CrossVersion.Full     = CrossVersion.Full("", "")
  val pluginConfig: Some[String]     = Some("plugin->default(compile)")

  val akka: InitialLib             = InitialLib("com.typesafe.akka:akka-actor:2.9.4", binaryJvm)
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

  val defaultRepositories: Seq[Repository] = Seq(Repositories.central)

  test("Integrated compiler plugin: kind projector") {
    val migrated  = LibraryMigration.migrateLib(kindProjector, defaultRepositories)
    val formatted = migrated.formatted
    val expected =
      s"""addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.12.0").cross(CrossVersion.full))""" +
        "\n" +
        s"""replaced by ${YELLOW}scalacOptions += "-Ykind-projector"$RESET"""
    assert(formatted == expected)
  }

  test("Incompatible compiler plugin: better monadic for") {
    val migrated  = LibraryMigration.migrateLib(betterMonadicFor, defaultRepositories)
    val formatted = migrated.formatted
    val expected  = s"""addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1") $RED(Compiler Plugin)$RESET"""
    assert(formatted == expected)
  }

  test("Java lib") {
    val migrated = LibraryMigration.migrateLib(opentelemetry, defaultRepositories)
    val expected = ValidLibrary(opentelemetry)
    assert(migrated == expected)
  }

  test("Java lib 2") {
    val migrated = LibraryMigration.migrateLib(javaLib2, defaultRepositories)
    val expected = ValidLibrary(javaLib2)
    assert(migrated == expected)
  }

  test("Available in scala 3") {
    val migrated = LibraryMigration.migrateLib(cats, defaultRepositories)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val version = migrated.asInstanceOf[UpdatedVersion].versions
    assert(version.head.toString == "2.6.1")
  }

  test("only available in an earlier version in maven central repository") {
    val migrated = LibraryMigration.migrateLib(akka, defaultRepositories)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val versions = migrated.asInstanceOf[UpdatedVersion].versions
    assert(versions.last == "2.9.0-M2", "The last version published to Maven Central was 2.9.0-M2")
  }

  test("available in scala 3 in another repository") {
    val repositories = defaultRepositories :+ MavenRepository("https://repo.akka.io/maven")
    val migrated     = LibraryMigration.migrateLib(akka, repositories)
    assert(migrated.isInstanceOf[ValidLibrary])
  }

  test("CrossVersion.Disabled to CrossVersion.Binary") {
    val migrated = LibraryMigration.migrateLib(cats213, defaultRepositories)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val updatedVersion = migrated.asInstanceOf[UpdatedVersion]
    assert(updatedVersion.versions.head.toString == "2.6.1")
    assert(updatedVersion.lib == cats)
  }
  test("Don't show older version") {
    val migrated = LibraryMigration.migrateLib(collectionCompat, defaultRepositories)
    assert(migrated.isInstanceOf[UpdatedVersion])
    val updatedVersions = migrated.asInstanceOf[UpdatedVersion].versions
    assert(!updatedVersions.contains("2.3.2"))
  }
  test("Cross compatible lib") {
    val migrated = LibraryMigration.migrateLib(scalafix, defaultRepositories)
    val expected = CrossCompatibleLibrary(scalafix)
    assert(migrated == expected)
  }
  // Warning: this test may change if the lib is ported to scala 3
  test("Incompatible because macro lib") {
    val migrated = LibraryMigration.migrateLib(macroLib, defaultRepositories)
    val expected = IncompatibleLibrary(macroLib, "Macro Library")
    assert(migrated == expected)
  }

  test("Filtered out libs") {
    val scalaLib     = InitialLib("org.scala-lang:scala-library:2.13.13", CrossVersion.Disabled)
    val scalajs      = InitialLib("org.scala-js:scalajs-compiler:1.5.0", CrossVersion.Disabled)
    val migratedLibs = LibraryMigration.migrateLibs(Seq(scalaLib, scalajs), defaultRepositories)
    assert(migratedLibs.isEmpty)
  }

  test("Formatting of updated versions") {
    val updatedVersions = UpdatedVersion(collectionCompat, Seq("2.4.4", "2.5.0", "2.6.0"))
    val formatted       = updatedVersions.formatted
    val expected =
      s""""org.scala-lang.modules" %% "scala-collection-compat" % "${YELLOW}2.4.4$RESET" $YELLOW(Other versions: 2.5.0, 2.6.0)$RESET"""
    assert(formatted == expected)
  }

  test("Formatting of valid Scala.js library") {
    val migratedLib = LibraryMigration.migrateLib(domtypes, defaultRepositories)
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
