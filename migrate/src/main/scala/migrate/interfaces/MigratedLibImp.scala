package migrate.interfaces

import migrate.interfaces.InitialLibImp.Revision
import migrate.interfaces.MigratedLibImp._
import migrate.internal.ScalaVersion

trait MigratedLibImp extends MigratedLib {
  def reason: Reason
  def isCompatibleWithScala3: Boolean
  def toString: String

  override def getReasonWhy: String = reason.why
}
object MigratedLibImp {
  sealed abstract class Reason(val why: String)

  object Reason {
    case object ScalacOptionEquivalent
        extends Reason("This compiler plugin has a scalacOption equivalent. Add it to your scalacOptions.")
    case object MacroLibrary extends Reason("Contains Macros and is not yet published for Scala 3.")
    case class FullVersionNotAvailable(scalaVersion: ScalaVersion)
        extends Reason(s"This dependency hasn't been published for ${scalaVersion.value}.")
    case object CompilerPlugin
        extends Reason("Scala 2 compiler plugins are not supported in scala 3. You need to find an alternative.")
    case class Scala3LibAvailable(otherRevisions: Seq[Revision])
        extends Reason(
          if (otherRevisions.nonEmpty) s"Other versions are avaialble for Scala 3: ${printRevisions(otherRevisions)}"
          else ""
        )
    case object JavaLibrary    extends Reason("Java libraries are compatible.")
    case object IsAlreadyValid extends Reason("")
    case object For3Use2_13    extends Reason("It's only safe to use the 2.13 version if it's inside an application.")

    private def printRevisions(r: Seq[Revision]): String =
      if (r.size >= 3) s""""${r.head.value}", ..., "${r.last.value}""""
      else r.map(_.value).mkString("\"", "\", \"", "\"")
  }
}
