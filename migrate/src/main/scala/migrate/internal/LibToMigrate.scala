package migrate.internal

import java.util.Optional

import scala.util.Try

import migrate.interfaces.Lib
import migrate.internal.Lib213.macroLibs
import migrate.internal.LibToMigrate._
import migrate.internal.ScalacOption.Specific3
import migrate.utils.CoursierHelper
import migrate.utils.ScalaExtensions._

sealed trait LibToMigrate extends Lib {
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

  def isCompilerPlugin: Boolean = configurations.contains("plugin->default(compile)")
}

case class Lib213(
  organization: Organization,
  name: Name,
  revision: Revision,
  crossVersion: CrossVersion,
  configurations: Option[String]
) extends LibToMigrate {
  def toCompatible: Either[Option[Scala3cOption], Seq[CompatibleWithScala3Lib]] = {
    val result = if (isCompilerPlugin) {
      val compatibleLib = CoursierHelper.getCompatibleForScala3Full(this)
      if (compatibleLib.isEmpty)
        Left(Lib213.compilerPluginToScalacOption.get((this.organization, this.name)))
      else Right(compatibleLib)
    } else {
      crossVersion match {
        case CrossVersion.Disabled => {
          this.name.value.split("_").toList match {
            case name :: scalaVersion :: Nil =>
              val crossVersion =
                if (scalaVersion.split('.').length == 2) CrossVersion.Binary("", "") else CrossVersion.Full("", "")
              val newLib213 = this.copy(name = Name(name))
              crossVersion match {
                case CrossVersion.Binary(_, _) => Right(getCompatibleWhenBinaryCrossVersion(newLib213))
                case _                         => Right(CoursierHelper.getCompatibleForScala3Full(newLib213))
              }
            // keep the same if CrossVersion.Disabled. Usually it's a Java Lib
            case _ => Right(Seq(CompatibleWithScala3Lib.from(this)))
          }
        }
        // look for revisions that are compatible with scala 3 binary version
        case CrossVersion.Binary(_, _) => Right(getCompatibleWhenBinaryCrossVersion(this))
        // look for revisions that are compatible with scala 3 full version
        case CrossVersion.Full(_, _) => Right(getCompatibleWhenFullCrossVersion(this))
        // already compatible
        case CrossVersion.For2_13Use3(_, _) => Right(Seq(CompatibleWithScala3Lib.from(this)))
        case CrossVersion.For3Use2_13(_, _) => Right(Seq(CompatibleWithScala3Lib.from(this)))
        // For Patch and Constant, we search full compatible scala 3 version
        case CrossVersion.Patch       => Right(CoursierHelper.getCompatibleForScala3Full(this))
        case CrossVersion.Constant(_) => Right(CoursierHelper.getCompatibleForScala3Full(this))
      }
    }
    result match {
      case Right(compatibleLibs) =>
        // we want to keep the first newer version and the last
        if (compatibleLibs.size <= 1)
          Right(compatibleLibs)
        else Right(Seq(compatibleLibs.head, compatibleLibs.last))
      case Left(value) => Left(value)
    }
  }

  override def toString: String =
    s"${organization.value}:${name.value}:${revision.value}${configurations.map(":" + _).getOrElse("")}"

  private def getCompatibleWhenBinaryCrossVersion(lib: Lib213): Seq[CompatibleWithScala3Lib] = {
    val scala3Libs = CoursierHelper.getCompatibleForScala3Binary(lib)
    if (scala3Libs.isEmpty) {
      if (macroLibs.get(lib.organization).contains(lib.name)) Nil
      else Seq(CompatibleWithScala3Lib.withCrossVersionFor3Use2_13(lib))
    } else scala3Libs
  }

  private def getCompatibleWhenFullCrossVersion(lib: Lib213): Seq[CompatibleWithScala3Lib] = {
    val scala3Libs = CoursierHelper.getCompatibleForScala3Full(lib)
    if (scala3Libs.isEmpty) {
      if (macroLibs.get(lib.organization).contains(lib.name)) Nil
      else
        Seq(
          CompatibleWithScala3Lib(
            lib.organization,
            Name(lib.name.value + s"_{REPLACE_BY_scala213_version}"),
            lib.revision,
            CrossVersion.For3Use2_13("", ""),
            lib.configurations
          )
        )
    } else scala3Libs
  }
}

case class CompatibleWithScala3Lib(
  organization: Organization,
  name: Name,
  revision: Revision,
  crossVersion: CrossVersion,
  configurations: Option[String]
) extends LibToMigrate {

  override def toString: String = {
    val configuration = configurations.map(c => " % " + withQuote(c)).getOrElse("")
    val orgQuoted     = withQuote(organization.value)
    withQuote(name.value)
    val revisionQuoted = withQuote(revision.value)
    crossVersion match {
      case CrossVersion.Disabled          => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
      case CrossVersion.For2_13Use3(_, _) => s"$orgQuoted %% ${withQuote(name.value)} % $revisionQuoted$configuration"
      case _                              => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
    }
  }
  private def withQuote(s: String) = "\"" + s + "\""
}

object LibToMigrate {
  case class Organization(value: String)
  case class Name(value: String)
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

object Lib213 {
  def from(lib: migrate.interfaces.Lib): Option[Lib213] = {
    val organization = Organization(lib.getOrganization)
    val name         = Name(lib.getName)
    val revision     = Revision(lib.getRevision)
    val crossVersion = CrossVersion.from(lib.getCrossVersion)
    crossVersion.map(c => Lib213(organization, name, revision, c, lib.getConfigurations.asScala))
  }

  def from(value: String, crossVersion: CrossVersion, configurations: Option[String] = None): Option[Lib213] = {
    val splited = value.split(":").toList
    splited match {
      case (org :: name :: revision :: Nil) =>
        Some(Lib213(Organization(org), Name(name), Revision(revision), crossVersion, configurations))
      case _ => None
    }
  }

  val compilerPluginToScalacOption: Map[(Organization, Name), Scala3cOption] =
    Map(
      (Organization("org.typelevel"), Name("kind-projector"))    -> Specific3.KindProjector,
      (Organization("org.scalameta"), Name("semanticdb-scalac")) -> Specific3.SemanticDB
    )

  val scalaLibrary: Lib213 = Lib213.from("org.scala-lang:scala-library:2.13.5", CrossVersion.Disabled, None).get

  val macroLibs: Map[Organization, Name] = {
    // need to complete the list
    // the other solution would be to download the src-jar and look for =\w*macro\w
    Map(
      Organization("com.softwaremill.scalamacrodebug")            -> Name("macros"),
      Organization("com.github.ajozwik")                          -> Name("macro"),
      Organization("io.argonaut")                                 -> Name("argonaut"),
      Organization("eu.timepit")                                  -> Name("refined"),
      Organization("org.backuity")                                -> Name("ansi-interpolator"),
      Organization("org.typelevel")                               -> Name("log4cats-slf4j"),
      Organization("org.typelevel")                               -> Name("log4cats-core"),
      Organization("com.github.dmytromitin")                      -> Name("auxify-macros"),
      Organization("biz.enef")                                    -> Name("slogging"),
      Organization("io.getquill")                                 -> Name("quill-jdbc"),
      Organization("com.phylage")                                 -> Name("refuel-container"),
      Organization("com.typesafe.scala-logging")                  -> Name("scala-logging"),
      Organization("com.lihaoyi")                                 -> Name("macro"),
      Organization("com.lihaoyi")                                 -> Name("fastparse"),
      Organization("com.github.kmizu")                            -> Name("macro_peg"),
      Organization("com.michaelpollmeier")                        -> Name("macros"),
      Organization("me.lyh")                                      -> Name("parquet-avro-extra"),
      Organization("org.spire-math")                              -> Name("imp"),
      Organization("com.typesafe.play")                           -> Name("play-json"),
      Organization("com.github.plokhotnyuk.expression-evaluator") -> Name("expression-evaluator"),
      Organization("com.github.plokhotnyuk.fsi")                  -> Name("fsi-macros"),
      Organization("com.propensive")                              -> Name("magnolia"),
      Organization("org.wvlet.airframe")                          -> Name("airframe"),
      Organization("com.wix")                                     -> Name("accord-api"),
      Organization("org.typelevel")                               -> Name("spire"),
      Organization("org.typelevel")                               -> Name("claimant"),
      Organization("com.softwaremill.macwire")                    -> Name("util"),
      Organization("com.typesafe.slick")                          -> Name("slick"),
      Organization("io.bullet")                                   -> Name("borer-core"),
      Organization("org.parboiled")                               -> Name("parboiled"),
      Organization("com.github.pureconfig")                       -> Name("pureconfig")
    )
  }
}

object CompatibleWithScala3Lib {
  def from(lib: Lib213): CompatibleWithScala3Lib =
    CompatibleWithScala3Lib(lib.organization, lib.name, lib.revision, lib.crossVersion, lib.configurations)
  def withCrossVersionFor3Use2_13(lib: Lib213): CompatibleWithScala3Lib =
    CompatibleWithScala3Lib(
      lib.organization,
      Name(lib.name.value + s"_${CoursierHelper.scala213Binary}"),
      lib.revision,
      CrossVersion.For3Use2_13("", ""),
      lib.configurations
    )
}
