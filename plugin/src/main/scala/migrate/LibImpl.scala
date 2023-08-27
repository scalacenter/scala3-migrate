package migrate

import migrate.interfaces.Lib
import sbt.librarymanagement.ModuleID
import sbt.sbtOptionSyntaxRichOption

import java.util.Optional

case class LibImpl(moduleId: ModuleID) extends Lib {
  override def getOrganization: String             = moduleId.organization
  override def getName: String                     = moduleId.name
  override def getVersion: String                  = moduleId.revision
  override def getCrossVersion: String             = moduleId.crossVersion.toString()
  override def getConfigurations: Optional[String] = moduleId.configurations.asJava

  override def toString: String =
    s"${this.getOrganization}:${this.getName}:${this.getVersion}"

  def isCompilerPlugin: Boolean = moduleId.configurations.contains("compile")
}

object LibImpl {
  def apply(lib: ModuleID): Lib = new LibImpl(lib)
}
