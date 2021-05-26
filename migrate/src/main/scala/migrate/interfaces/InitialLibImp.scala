package migrate.interfaces

import java.util.Optional

import scala.util.Try

import migrate.interfaces.InitialLibImp._
import migrate.utils.ScalaExtensions._

trait InitialLibImp extends Lib {
  val organization: Organization
  val name: Name
  val revision: Revision
  val crossVersion: CrossVersion
  val configurations: Option[String]

  override def getOrganization: String             = organization.value
  override def getName: String                     = name.value
  override def getRevision: String                 = revision.value
  override def getCrossVersion: String             = crossVersion.toString
  override def getConfigurations: Optional[String] = configurations.asJava
  override def isCompilerPlugin: Boolean           = configurations.contains("plugin->default(compile)")
}

object InitialLibImp {
  case class Organization(value: String)

  case class Name(value: String) {
    def scalaVersion: Option[String] = value.split("_").toList match {
      case _ :: scalaVersion :: Nil => Some(scalaVersion)
      case _                        => None
    }
  }

  case class Revision(value: String) {
    private val version: Seq[String] = value.split('.').toSeq
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
