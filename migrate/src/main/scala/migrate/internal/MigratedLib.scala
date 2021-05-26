package migrate.internal

import migrate.interfaces.InitialLibImp._
import migrate.interfaces.MigratedLibImp
import migrate.interfaces.MigratedLibImp._

object MigratedLib {
  sealed trait CompatibleWithScala3 extends MigratedLibImp {
    override def isCompatibleWithScala3: Boolean = true
  }

  object CompatibleWithScala3 {
    case class ScalacOption(value: Scala3cOption) extends CompatibleWithScala3 {
      override def reason: Reason = Reason.ScalacOptionEquivalent

      override def toString: String = value.scala3Value
    }

    case class Scala3Lib(
      organization: Organization,
      name: Name,
      revisions: Seq[Revision],
      crossVersion: CrossVersion,
      configurations: Option[String],
      reason: Reason
    ) extends CompatibleWithScala3 {
      val revision = revisions.head // todo: change

      override def toString: String = {
        val configuration = configurations.map(c => " % " + withQuote(c)).getOrElse("")
        val orgQuoted     = withQuote(organization.value)
        withQuote(name.value)
        val revisionQuoted = withQuote(revision.value)
        crossVersion match {
          case CrossVersion.Disabled => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
          case CrossVersion.For2_13Use3(_, _) =>
            s"$orgQuoted %% ${withQuote(name.value)} % $revisionQuoted$configuration"
          case _ => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
        }
      }

      private def withQuote(s: String) = "\"" + s + "\""
    }

    case class KeptInitialLib(
      organization: Organization,
      name: Name,
      revision: Revision,
      crossVersion: CrossVersion,
      configurations: Option[String],
      reason: Reason
    ) extends CompatibleWithScala3 {

      override def toString: String = {
        val configuration = configurations.map(c => " % " + withQuote(c)).getOrElse("")
        val orgQuoted     = withQuote(organization.value)
        withQuote(name.value)
        val revisionQuoted = withQuote(revision.value)
        crossVersion match {
          case CrossVersion.Disabled => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
          case CrossVersion.For2_13Use3(_, _) =>
            s"$orgQuoted %% ${withQuote(name.value)} % $revisionQuoted$configuration"
          case _ => s"$orgQuoted % ${withQuote(name.value)} % $revisionQuoted$configuration"
        }
      }

      private def withQuote(s: String) = "\"" + s + "\""
    }

  }

  case class UncompatibleWithScala3(
    organization: Organization,
    name: Name,
    revision: Revision,
    crossVersion: CrossVersion,
    configurations: Option[String],
    reason: Reason
  ) extends MigratedLibImp {
    override def isCompatibleWithScala3: Boolean = false
  }

  object CompatibleWithScala3Lib {
    def from(lib: InitialLib, reason: Reason): CompatibleWithScala3.Scala3Lib =
      CompatibleWithScala3.Scala3Lib(
        lib.organization,
        lib.name,
        Seq(lib.revision),
        lib.crossVersion,
        lib.configurations,
        reason
      )

  }
}
