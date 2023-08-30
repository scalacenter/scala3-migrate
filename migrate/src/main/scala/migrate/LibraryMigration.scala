package migrate

import migrate.buildinfo.BuildInfo
import migrate.interfaces.MigratedLib
import migrate.interfaces.MigratedLibs
import migrate.internal.CrossCompatibleLibrary
import migrate.internal.CrossVersion
import migrate.internal.IncompatibleLibrary
import migrate.internal.InitialLib
import migrate.internal.IntegratedPlugin
import migrate.internal.UnclassifiedLibrary
import migrate.internal.UpdatedVersion
import migrate.internal.ValidLibrary
import migrate.utils.CoursierHelper

object LibraryMigration {
  def migrateLibs(libs: Seq[InitialLib]): MigratedLibs = {
    val filteredLibs = libs.filterNot(l => InitialLib.migrationFilter.contains((l.organization, l.name)))
    val migratedLibs = filteredLibs.map(migrateLib)

    val validLibs                = migratedLibs.collect { case l: ValidLibrary => l }.toArray[MigratedLib]
    val updatedVersions          = migratedLibs.collect { case l: UpdatedVersion => l }.toArray[MigratedLib]
    val crossCompatibleLibraries = migratedLibs.collect { case l: CrossCompatibleLibrary => l }.toArray[MigratedLib]
    val integratedPlugins        = migratedLibs.collect { case l: IntegratedPlugin => l }.toArray[MigratedLib]
    val unclassifiedLibraries    = migratedLibs.collect { case l: UnclassifiedLibrary => l }.toArray[MigratedLib]
    val incompatibleLibraries    = migratedLibs.collect { case l: IncompatibleLibrary => l }.toArray[MigratedLib]

    new MigratedLibs(
      validLibs,
      updatedVersions,
      crossCompatibleLibraries,
      integratedPlugins,
      unclassifiedLibraries,
      incompatibleLibraries)
  }

  def migrateLib(lib: InitialLib): MigratedLib =
    if (lib.isCompilerPlugin) migrateCompilerPlugin(lib)
    else migrateRegularLib(lib)

  def migrateRegularLib(lib: InitialLib): MigratedLib =
    lib.crossVersion match {
      case CrossVersion.Disabled       => tryParseBinaryVersionAndMigrate(lib)
      case _: CrossVersion.For2_13Use3 => ValidLibrary(lib)
      case _: CrossVersion.For3Use2_13 => ValidLibrary(lib)
      case _: CrossVersion.Binary      => migrateBinaryVersion(lib)
      case _: CrossVersion.Full        => migrateFullCrossVersion(lib)
      case CrossVersion.Patch          => migrateFullCrossVersion(lib)
      case CrossVersion.Constant("2.13") =>
        migrateBinaryVersion(lib.copy(crossVersion = CrossVersion.Binary("", "")))
      case CrossVersion.Constant(s"2.13.$_") =>
        migrateFullCrossVersion(lib.copy(crossVersion = CrossVersion.Binary("", "")))
      case cv: CrossVersion.Constant => UnclassifiedLibrary(lib, s"Unsupported CrossVersion.$cv")
    }

  private def tryParseBinaryVersionAndMigrate(lib: InitialLib): MigratedLib =
    lib.name.split("_").toList match {
      case name :: suffix :: Nil =>
        suffix match {
          case "2.13" =>
            val newLib = lib.copy(name = name, crossVersion = CrossVersion.Binary("", ""))
            migrateBinaryVersion(newLib)
          case s"2.13.$_" =>
            val newLib = lib.copy(name = name, crossVersion = CrossVersion.Full("", ""))
            migrateFullCrossVersion(newLib)
          case _ => ValidLibrary(lib)
        }
      case _ => ValidLibrary(lib)
    }

  private def migrateBinaryVersion(lib: InitialLib): MigratedLib =
    if (CoursierHelper.isCompatible(lib, "3")) ValidLibrary(lib)
    else {
      CoursierHelper.findNewerVersions(lib, "3").toList match {
        case Nil =>
          if (lib.isCompilerPlugin) IncompatibleLibrary(lib, "Compiler Plugin")
          else if (InitialLib.macroLibraries.contains(lib.organization, lib.name))
            IncompatibleLibrary(lib, "Macro Library")
          else CrossCompatibleLibrary(lib)
        case newerVersions => UpdatedVersion(lib, newerVersions)
      }
    }

  private def migrateFullCrossVersion(lib: InitialLib): MigratedLib =
    CoursierHelper.findNewerVersions(lib, BuildInfo.scala3Version) match {
      case Nil =>
        val reason =
          if (lib.isCompilerPlugin) "Compiler plugin"
          else "Full cross-version"
        IncompatibleLibrary(lib, reason)
      case newerVersions => UpdatedVersion(lib, newerVersions)
    }

  private def migrateCompilerPlugin(lib: InitialLib): MigratedLib =
    (lib.organization, lib.name) match {
      case ("org.typelevel", "kind-projector") => IntegratedPlugin(lib, "-Ykind-projector")
      case (_, _)                              => migrateRegularLib(lib)
    }
}
