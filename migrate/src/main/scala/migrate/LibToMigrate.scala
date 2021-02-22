package migrate

import scala.util.Try

import migrate.Lib213.macroLibs
import migrate.LibToMigrate._
import migrate.interfaces.Lib
import migrate.utils.CoursierHelper

sealed trait LibToMigrate extends Lib {
  val organization: Organization
  val name: Name
  val revision: Revision
  val crossVersion: CrossVersion

  override def getOrganization: String = organization.value
  override def getName: String         = name.value
  override def getRevision: String     = revision.value
  override def getCrossVersion: String = crossVersion.toString

  override def toString: String = s"${organization.value}:${name.value}:${revision.value}"

}

case class Lib213(organization: Organization, name: Name, revision: Revision, crossVersion: CrossVersion)
    extends LibToMigrate {
  def toCompatible: Seq[CompatibleWithScala3Lib] =
    crossVersion match {
      // keep the same if CrossVersion.Disabled. Usually it's a Java Lib
      case CrossVersion.Disabled => Seq(CompatibleWithScala3Lib.from(this))
      // look for revisions that are compatible with scala 3 binary version
      case CrossVersion.Binary(_, _) => getCompatibleWhenBinaryCrossVersion()
      // look for revisions that are compatible with scala 3 full version
      case CrossVersion.Full(_, _) => CoursierHelper.getCompatibleForScala3Full(this)
      // already compatible
      case CrossVersion.For2_13Use3(_, _) => Seq(CompatibleWithScala3Lib.from(this))
      case CrossVersion.For3Use2_13(_, _) => Seq(CompatibleWithScala3Lib.from(this))
      // For Patch and Constant, we search full compatible scala 3 version
      case CrossVersion.Patch       => CoursierHelper.getCompatibleForScala3Full(this)
      case CrossVersion.Constant(_) => CoursierHelper.getCompatibleForScala3Full(this)
    }

  private def getCompatibleWhenBinaryCrossVersion(): Seq[CompatibleWithScala3Lib] = {
    val scala3Libs = CoursierHelper.getCompatibleForScala3Binary(this)
    if (scala3Libs.isEmpty) {
      if (macroLibs.get(this.organization).contains(this.name)) Nil
      else CoursierHelper.getCompatibleForBinary213(this)
    } else scala3Libs
  }

}

case class CompatibleWithScala3Lib(
  organization: Organization,
  name: Name,
  revision: Revision,
  crossVersion: CrossVersion
) extends LibToMigrate

object LibToMigrate {
  case class Organization(value: String)
  case class Name(value: String)
  case class Revision(value: String) {
    private val version: Seq[String] = value.split('.')
    val major: Option[Int]           = version.headOption.flatMap(v => Try(v.toInt).toOption)
    val minor: Option[Int]           = Try(version(1).toInt).toOption
    val patch: Option[Int]           = Try(version(2).split("-")(0).toInt).toOption
    val beta: Option[String]         = Try(version(2).split("-")(1)).toOption

  }
  sealed trait CrossVersion {
    override def toString: String = this match {
      case CrossVersion.Binary(prefix: String, suffix: String)      => s"Binary($prefix, $suffix)"
      case CrossVersion.Disabled                                    => "Disabled()"
      case CrossVersion.Constant(value: String)                     => s"Constant($value)"
      case CrossVersion.Patch                                       => "Patch()"
      case CrossVersion.Full(prefix: String, suffix: String)        => s"Full($prefix, $suffix)"
      case CrossVersion.For3Use2_13(prefix: String, suffix: String) => s"For3Use2_13($prefix, $suffix)"
      case CrossVersion.For2_13Use3(prefix: String, suffix: String) => s"For3Use2_13($prefix, $suffix)"
    }
  }

  object CrossVersion {
    case class Binary(prefix: String, suffix: String)      extends CrossVersion
    case object Disabled                                   extends CrossVersion
    case class Constant(value: String)                     extends CrossVersion
    case object Patch                                      extends CrossVersion
    case class Full(prefix: String, suffix: String)        extends CrossVersion
    case class For3Use2_13(prefix: String, suffix: String) extends CrossVersion
    case class For2_13Use3(prefix: String, suffix: String) extends CrossVersion

    def from(value: String): Option[CrossVersion] =
      value match {
        case "Disabled()"                     => Some(Disabled)
        case s"Binary($prefix, $suffix)"      => Some(Binary(prefix, suffix))
        case s"Constant($value)"              => Some(Constant(value))
        case "Patch()"                        => Some(Patch)
        case s"Full($prefix, $suffix)"        => Some(Full(prefix, suffix))
        case s"For3Use2_13($prefix, $suffix)" => Some(For3Use2_13(prefix, suffix))
        case s"For3Use2_13($prefix, $suffix)" => Some(For2_13Use3(prefix, suffix))
        case _                                => None
      }
  }
}

object Lib213 {
  def from(lib: migrate.interfaces.Lib): Option[Lib213] = {
    val organization = Organization(lib.getOrganization)
    val name         = Name(lib.getName)
    val revision     = Revision(lib.getRevision)
    val crossVersion = CrossVersion.from(lib.getCrossVersion)
    crossVersion.map(c => Lib213(organization, name, revision, c))
  }

  def from(value: String, crossVersion: CrossVersion): Option[Lib213] = {
    val splited = value.split(":").toList
    splited match {
      case (org :: name :: revision :: Nil) =>
        Some(Lib213(Organization(org), Name(name), Revision(revision), crossVersion))
      case _ => None
    }
  }

  val macroLibs: Map[Organization, Name] =
    // need to complete the list
    Map(
      Organization("com.softwaremill.scalamacrodebug") -> Name("macros"),
      Organization("com.github.ajozwik")               -> Name("macro")
    )
}

object CompatibleWithScala3Lib {
  def from(lib: Lib213): CompatibleWithScala3Lib =
    CompatibleWithScala3Lib(lib.organization, lib.name, lib.revision, lib.crossVersion)
}
