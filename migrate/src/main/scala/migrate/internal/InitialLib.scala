package migrate.internal

import migrate.interfaces.InitialLibImp
import migrate.interfaces.InitialLibImp._
import migrate.interfaces.MigratedLibImp
import migrate.interfaces.MigratedLibImp._
import migrate.internal.InitialLib.macroLibs
import migrate.internal.MigratedLib._
import migrate.internal.ScalacOption.Specific3
import migrate.utils.CoursierHelper
import migrate.utils.ScalaExtensions._

// This lib can be a 2.13, a java library, or Scala 3 library.
case class InitialLib(
  organization: Organization,
  name: Name,
  revision: Revision,
  crossVersion: CrossVersion,
  configurations: Option[String]
) extends InitialLibImp {

  def toCompatible: MigratedLibImp =
    if (isCompilerPlugin) getMigratedLibForCompilerPlugin()
    else
      crossVersion match {
        case CrossVersion.Disabled =>
          name.scalaVersion match {
            case Some(value) if value.split('.').size == 2 =>
              val modifiedName = name.value.split("_").head
              getCompatibleWhenBinaryCrossVersion(copy(name = Name(modifiedName)))
            case Some(_) =>
              val modifiedName = name.value.split("_").head
              CoursierHelper
                .getCompatibleForScala3Full(copy(name = Name(modifiedName)))
                .getOrElse(toUncompatible(Reason.FullVersionNotAvailable))
            case None => keepTheSameLib(Reason.JavaLibrary)
          }

        // look for revisions that are compatible with scala 3 binary version
        case CrossVersion.Binary(_, _) => getCompatibleWhenBinaryCrossVersion(this)
        // look for revisions that are compatible with scala 3 full version
        case CrossVersion.Full(_, _) => getCompatibleWhenFullCrossVersion(this)
        // already compatible
        case CrossVersion.For2_13Use3(_, _) => keepTheSameLib(Reason.IsAlreadyValid)
        case CrossVersion.For3Use2_13(_, _) => keepTheSameLib(Reason.IsAlreadyValid)
        // For Patch and Constant, we search full compatible scala 3 version
        case CrossVersion.Patch =>
          CoursierHelper
            .getCompatibleForScala3Full(this)
            .getOrElse(this.toUncompatible(Reason.FullVersionNotAvailable))
        case CrossVersion.Constant(_) =>
          CoursierHelper
            .getCompatibleForScala3Full(this)
            .getOrElse(this.toUncompatible(Reason.FullVersionNotAvailable))
      }

  private def toUncompatible(reason: Reason): UncompatibleWithScala3 =
    UncompatibleWithScala3(organization, name, revision, crossVersion, configurations, reason)

  private def keepTheSameLib(reason: Reason): CompatibleWithScala3.Lib =
    CompatibleWithScala3.Lib(organization, name, Seq(revision), crossVersion, configurations, reason)

  private def for3Use2_13(reason: Reason): CompatibleWithScala3.Lib =
    CompatibleWithScala3.Lib(
      organization,
      name,
      Seq(revision),
      CrossVersion.For3Use2_13("", ""),
      configurations,
      reason
    )

  override def toString: String =
    s"${organization.value}:${name.value}:${revision.value}${configurations.map(":" + _).getOrElse("")}"

  private def getMigratedLibForCompilerPlugin(): MigratedLibImp =
    CoursierHelper.getCompatibleForScala3Full(this) match {
      case Some(scla3Lib) => scla3Lib
      case None =>
        val scalacOption = InitialLib.compilerPluginToScalacOption.get((this.organization, this.name))
        scalacOption match {
          case Some(value) => CompatibleWithScala3.ScalacOption(value)
          case None        => this.toUncompatible(Reason.CompilerPlugin)
        }
    }

  private def getCompatibleWhenBinaryCrossVersion(lib: InitialLib): MigratedLibImp = {
    val scala3Lib = CoursierHelper.getCompatibleForScala3Binary(lib)
    scala3Lib match {
      case None =>
        if (macroLibs.get(lib.organization).contains(lib.name)) lib.toUncompatible(Reason.MacroLibrary)
        else lib.for3Use2_13(Reason.For3Use2_13)
      case Some(compatible) => compatible
    }
  }

  private def getCompatibleWhenFullCrossVersion(lib: InitialLib): MigratedLibImp =
    CoursierHelper.getCompatibleForScala3Full(lib) match {
      case Some(_) =>
        CompatibleWithScala3.Lib(
          lib.organization,
          Name(lib.name.value),
          Seq(lib.revision),
          CrossVersion.For2_13Use3("", ""),
          lib.configurations,
          Reason.Scala3LibAvailable
        )
      case None => lib.toUncompatible(Reason.FullVersionNotAvailable)
    }
}

object InitialLib {
  def from(lib: migrate.interfaces.Lib): Option[InitialLib] = {
    val organization = Organization(lib.getOrganization)
    val name         = Name(lib.getName)
    val revision     = Revision(lib.getRevision)
    val crossVersion = CrossVersion.from(lib.getCrossVersion)
    crossVersion.map(c => InitialLib(organization, name, revision, c, lib.getConfigurations.asScala))
  }

  def from(value: String, crossVersion: CrossVersion, configurations: Option[String] = None): Option[InitialLib] = {
    val splited = value.split(":").toList
    splited match {
      case (org :: name :: revision :: Nil) =>
        Some(InitialLib(Organization(org), Name(name), Revision(revision), crossVersion, configurations))
      case _ => None
    }
  }

  val compilerPluginToScalacOption: Map[(Organization, Name), Scala3cOption] =
    Map(
      (Organization("org.typelevel"), Name("kind-projector"))    -> Specific3.KindProjector,
      (Organization("org.scalameta"), Name("semanticdb-scalac")) -> Specific3.SemanticDB
    )

  val scalaLibrary: InitialLib =
    InitialLib.from("org.scala-lang:scala-library:2.13.5", CrossVersion.Disabled, None).get

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
      Organization("com.github.pureconfig")                       -> Name("pureconfig"),
      Organization("com.geirsson")                                -> Name("metaconfig-typesafe-config")
    )
  }
}
