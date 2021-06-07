package migrate.interfaces

import migrate.interfaces.MigratedLibImp._

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
        extends Reason("This compiler plugin has a scalacOption equivalent. Add it to your scalacOptions")
    case object MacroLibrary            extends Reason("Contains Macros and is not yet published for Scala 3")
    case object FullVersionNotAvailable extends Reason("Requires a full version and is not yet for scala 3 ")
    case object CompilerPlugin
        extends Reason("Scala 2 compiler plugins are not supported in scala 3. You need to find an alternative")
    case object Scala3LibAvailable extends Reason("This dependency is published for Scala 3. ")
    case object JavaLibrary        extends Reason("This dependency is a Java library. It can be kept as it is. ")
    case object IsAlreadyValid     extends Reason("")
    case object For3Use2_13
        extends Reason("This dependency is not yet published for scala 3 but the 2.13 version can be used")
  }
}
