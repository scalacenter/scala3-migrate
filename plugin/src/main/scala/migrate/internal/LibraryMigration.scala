package migrate.internal

import coursier.core.Repository
import sbt.librarymanagement._
import migrate.BuildInfo

private[migrate] object LibraryMigration {
  def migrateLibs(libs: Seq[InitialLib], repositories: Seq[Repository]): Seq[MigratedLib] = {
    val filteredLibs = libs.filterNot(l => InitialLib.migrationFilter.contains((l.organization, l.name)))
    filteredLibs.map(migrateLib(_, repositories))
  }

  def migrateLib(lib: InitialLib, repositories: Seq[Repository]): MigratedLib =
    if (lib.isCompilerPlugin) migrateCompilerPlugin(lib, repositories)
    else migrateRegularLib(lib, repositories)

  def migrateRegularLib(lib: InitialLib, repositories: Seq[Repository]): MigratedLib = {
    val FullBinaryVersion = "2\\.13\\..*"
    lib.crossVersion match {
      case Disabled       => tryParseBinaryVersionAndMigrate(lib, repositories)
      case _: For2_13Use3 => ValidLibrary(lib)
      case _: For3Use2_13 => ValidLibrary(lib)
      case _: Binary      => migrateBinaryVersion(lib, repositories)
      case _: Full        => migrateFullCrossVersion(lib, repositories)
      case _: Patch       => migrateFullCrossVersion(lib, repositories)
      case v: Constant if v.value == "2.13" =>
        migrateBinaryVersion(lib.copy(crossVersion = Binary("", "")), repositories)
      case v: Constant if v.value.matches("2\\.13\\..*") =>
        migrateFullCrossVersion(lib.copy(crossVersion = Binary("", "")), repositories)
      case cv: Constant => UnclassifiedLibrary(lib, s"Unsupported CrossVersion.$cv")
    }
  }

  private def tryParseBinaryVersionAndMigrate(lib: InitialLib, repositories: Seq[Repository]): MigratedLib = {
    val FullBinaryVersion = "2\\.13\\..*".r
    lib.name.split("_").toList match {
      case name :: suffix :: Nil =>
        suffix match {
          case "2.13" =>
            val newLib = lib.copy(name = name, crossVersion = CrossVersion.Binary("", ""))
            migrateBinaryVersion(newLib, repositories)
          case FullBinaryVersion(_) =>
            val newLib = lib.copy(name = name, crossVersion = CrossVersion.Full("", ""))
            migrateFullCrossVersion(newLib, repositories)
          case _ => ValidLibrary(lib)
        }
      case _ => ValidLibrary(lib)
    }
  }

  private def migrateBinaryVersion(lib: InitialLib, repositories: Seq[Repository]): MigratedLib =
    if (CoursierHelper.isCompatible(lib, "3", repositories)) ValidLibrary(lib)
    else {
      CoursierHelper.findNewerVersions(lib, "3", repositories).toList match {
        case Nil =>
          if (lib.isCompilerPlugin) IncompatibleLibrary(lib, "Compiler Plugin")
          else if (InitialLib.macroLibraries.contains(lib.organization, lib.name))
            IncompatibleLibrary(lib, "Macro Library")
          else CrossCompatibleLibrary(lib)
        case newerVersions => UpdatedVersion(lib, newerVersions)
      }
    }

  private def migrateFullCrossVersion(lib: InitialLib, repositories: Seq[Repository]): MigratedLib =
    CoursierHelper.findNewerVersions(lib, BuildInfo.scala3Version, repositories) match {
      case Nil =>
        val reason =
          if (lib.isCompilerPlugin) "Compiler plugin"
          else "Full cross-version"
        IncompatibleLibrary(lib, reason)
      case newerVersions => UpdatedVersion(lib, newerVersions)
    }

  private def migrateCompilerPlugin(lib: InitialLib, repositories: Seq[Repository]): MigratedLib =
    (lib.organization, lib.name) match {
      case ("org.typelevel", "kind-projector") => IntegratedPlugin(lib, "-Ykind-projector")
      case (_, _)                              => migrateRegularLib(lib, repositories)
    }
}
