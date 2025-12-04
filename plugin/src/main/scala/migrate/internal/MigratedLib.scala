package migrate.internal

import scala.Console._
import sbt.librarymanagement._

trait MigratedLib {
  def formatted: String
}

case class ValidLibrary(
  lib: InitialLib
) extends MigratedLib {
  override def formatted: String =
    MigratedLibFormatting.formatLibrary(lib)
}

case class UpdatedVersion(
  lib: InitialLib,
  versions: Seq[String]
) extends MigratedLib {
  override def formatted: String =
    MigratedLibFormatting.formatLibrary(
      lib.organization,
      lib.name,
      s"$YELLOW${versions.head}$RESET",
      lib.crossVersion,
      lib.configurations) + otherVersions

  private def otherVersions: String =
    versions.tail.toList match {
      case Nil                 => ""
      case head :: Nil         => s" $YELLOW(Other version: $head)$RESET"
      case head :: last :: Nil => s" $YELLOW(Other versions: $head, $last)$RESET"
      case head :: tail        => s" $YELLOW(Other versions: $head, ..., ${tail.last})$RESET"
    }
}

case class CrossCompatibleLibrary(lib: InitialLib) extends MigratedLib {
  override def formatted: String = {
    val formattedConfigs = lib.configurations.map(c => " % " + MigratedLibFormatting.formatConfigs(c)).getOrElse("")
    lib.crossVersion match {
      case v: Binary if v.prefix == "" && v.suffix == "" =>
        s"""("${lib.organization}" %% "${lib.name}" % "${lib.version}"$formattedConfigs)$YELLOW.cross(CrossVersion.for3Use2_13)$RESET"""
      case _: Binary =>
        s"""("${lib.organization}" %%% "${lib.name}" % "${lib.version}"$formattedConfigs)$YELLOW.cross(CrossVersion.for3Use2_13)$RESET"""
      case _ =>
        s"""("${lib.organization}" %% "${lib.name}" % "${lib.version}"$formattedConfigs)$YELLOW.cross(CrossVersion.for3Use2_13)$RESET"""
    }
  }
}

case class IntegratedPlugin(lib: InitialLib, scalacOption: String) extends MigratedLib {
  override def formatted: String =
    MigratedLibFormatting.formatLibrary(lib) +
      "\n" +
      s"""replaced by ${YELLOW}scalacOptions += "$scalacOption"$RESET"""
}

case class UnclassifiedLibrary(lib: InitialLib, reason: String) extends MigratedLib {
  override def formatted: String = MigratedLibFormatting.formatLibrary(lib) + s" $YELLOW($reason)$RESET"
}

case class IncompatibleLibrary(lib: InitialLib, reason: String) extends MigratedLib {
  override def formatted: String = MigratedLibFormatting.formatLibrary(lib) + s" $RED($reason)$RESET"
}

object MigratedLibFormatting {
  def formatLibrary(lib: InitialLib): String =
    formatLibrary(lib.organization, lib.name, lib.version.toString, lib.crossVersion, lib.configurations)

  def formatLibrary(
    org: String,
    name: String,
    version: String,
    crossVersion: CrossVersion,
    configs: Option[String]): String =
    if (configs.contains("plugin->default(compile)"))
      s"addCompilerPlugin(${formatRegularLibrary(org, name, version, crossVersion, None)})"
    else formatRegularLibrary(org, name, version, crossVersion, configs)

  def formatRegularLibrary(
    org: String,
    name: String,
    version: String,
    crossVersion: CrossVersion,
    configs: Option[String]): String = {
    val formattedConfigs = configs.map(c => " % " + formatConfigs(c)).getOrElse("")
    crossVersion match {
      case v: Binary if v.prefix == "" && v.suffix == "" => s""""$org" %% "$name" % "$version"$formattedConfigs"""
      case _: Binary                                     => s""""$org" %%% "$name" % "$version"$formattedConfigs"""
      case Disabled                                      => s""""$org" % "$name" % "$version"$formattedConfigs"""
      case crossVersion                                  =>
        val crossVersionFmt = crossVersion match {
          case _: Full        => "full"
          case _: For3Use2_13 => "for3Use2_13"
          case _: For2_13Use3 => "for2_13Use3"
          case other          => other.toString
        }
        s"""("$org" %% "$name" % "$version"$formattedConfigs).cross(CrossVersion.$crossVersionFmt)"""
    }
  }

  def formatConfigs(configs: String): String =
    configs match {
      case "test"    => "Test"
      case "it"      => "IntegrationTest"
      case "compile" => "Compile"
      case configs   => s""""$configs""""
    }
}
