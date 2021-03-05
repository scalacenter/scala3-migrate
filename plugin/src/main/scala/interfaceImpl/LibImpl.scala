package interfaceImpl

import migrate.interfaces.Lib
import sbt.librarymanagement.ModuleID

case class LibImpl(lib: ModuleID) extends Lib {
  override def getOrganization: String   = lib.organization
  override def getName: String           = lib.name
  override def getRevision: String       = lib.revision
  override def getCrossVersion: String   = lib.crossVersion.toString()
  override def isCompilerPlugin: Boolean = lib.configurations.isDefined

  override def toString: String = s"${this.getOrganization}:${this.getName}:${this.getRevision}"
}
