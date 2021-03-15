package migrate

import java.util.Optional

import scala.util.Try

import migrate.Lib213.macroLibs
import migrate.LibToMigrate._
import migrate.ScalacOption.Specific3
import migrate.interfaces.Lib
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
  def toCompatible: Either[Option[Scala3cOption], Seq[CompatibleWithScala3Lib]] =
    if (isCompilerPlugin) {
      val compatibleLib = CoursierHelper.getCompatibleForScala3Full(this)
      if (compatibleLib.isEmpty)
        Left(Lib213.compilerPluginToScalacOption.get((this.organization, this.name)))
      else Right(compatibleLib)
    } else
      crossVersion match {
        // keep the same if CrossVersion.Disabled. Usually it's a Java Lib
        case CrossVersion.Disabled => Right(Seq(CompatibleWithScala3Lib.from(this)))
        // look for revisions that are compatible with scala 3 binary version
        case CrossVersion.Binary(_, _) => Right(getCompatibleWhenBinaryCrossVersion())
        // look for revisions that are compatible with scala 3 full version
        case CrossVersion.Full(_, _) => Right(CoursierHelper.getCompatibleForScala3Full(this))
        // already compatible
        case CrossVersion.For2_13Use3(_, _) => Right(Seq(CompatibleWithScala3Lib.from(this)))
        case CrossVersion.For3Use2_13(_, _) => Right(Seq(CompatibleWithScala3Lib.from(this)))
        // For Patch and Constant, we search full compatible scala 3 version
        case CrossVersion.Patch       => Right(CoursierHelper.getCompatibleForScala3Full(this))
        case CrossVersion.Constant(_) => Right(CoursierHelper.getCompatibleForScala3Full(this))
      }

  override def toString: String =
    s"${organization.value}:${name.value}:${revision.value}${configurations.map(":" + _).getOrElse("")}"

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
  crossVersion: CrossVersion,
  configurations: Option[String]
) extends LibToMigrate {

  override def toString: String = {
    val configuration  = configurations.map(c => " % " + withQuote(c)).getOrElse("")
    val orgQuoted      = withQuote(organization.value)
    val nameQuoted     = withQuote(name.value)
    val revisionQuoted = withQuote(revision.value)
    crossVersion match {
      case CrossVersion.Disabled          => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
      case CrossVersion.For2_13Use3(_, _) => s"$orgQuoted %% ${withQuote(name.value)} % $revisionQuoted$configuration"
      case CrossVersion.For3Use2_13(_, _) =>
        s"$orgQuoted % ${withQuote(s"${name.value}_${CoursierHelper.scala213Binary}")} % $revisionQuoted$configuration"
      case CrossVersion.Full(_, _) =>
        s"$orgQuoted % ${withQuote(s"${name.value}_${CoursierHelper.scala3Full}")} % $revisionQuoted$configuration"
      case _ => s"$orgQuoted %% $nameQuoted % $revisionQuoted$configuration"
    }
  }
  private def withQuote(s: String) = "\"" + s + "\""
}

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
}
