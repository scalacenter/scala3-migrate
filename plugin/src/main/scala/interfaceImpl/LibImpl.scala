package interfaceImpl

import migrate.interfaces.Lib
import sbt.librarymanagement.ModuleID
import sbt.sbtOptionSyntaxRichOption

import java.util.Optional

case class LibImpl(lib: ModuleID) extends Lib {
  override def getOrganization: String             = lib.organization
  override def getName: String                     = lib.name
  override def getRevision: String                 = lib.revision
  override def getCrossVersion: String             = lib.crossVersion.toString()
  override def getConfigurations: Optional[String] = lib.configurations.asJava

  override def toString: String =
    s"${this.getOrganization}:${this.getName}:${this.getRevision}"

  def isCompilerPlugin: Boolean = lib.configurations.contains("compile")
}
