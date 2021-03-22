package migrate.interfaces

import java.{ util => jutil }

import scala.jdk.CollectionConverters._

import migrate.internal.CompatibleWithScala3Lib
import migrate.internal.LibToMigrate
import migrate.internal.Scala3cOption

case class MigratedLibsImpl(
  libs: Map[Lib213, Seq[CompatibleWithScala3Lib]],
  compilerPlugins: Map[Lib213, Option[Scala3cOption]]
) extends MigratedLibs {
  private val (nonMigratedLibs, migrated) = libs.partition { case (_, migrated) => migrated.isEmpty }
  private val (compilerPluginsWithScalacOption, compilerPluginsWithout) = compilerPlugins.partition {
    case (_, scalacOption) =>
      scalacOption.isDefined
  }
  private val (validLibs, toUpdate) = migrated.partition { case (initial, compatible) =>
    compatible.exists(initialLibSameThanCompatible(initial, _))
  }

  override def getNotMigrated: Array[Lib] =
    (nonMigratedLibs.keys ++ compilerPluginsWithout.keys).map(_.asInstanceOf[Lib]).toArray

  override def getLibsToUpdate: jutil.Map[Lib, jutil.List[Lib]] =
    // to Java ^^
    toUpdate.map { case (initial, compatible) =>
      initial.asInstanceOf[Lib] -> compatible.map(_.asInstanceOf[Lib]).asJava
    }.asJava

  override def getValidLibs: Array[Lib] = validLibs.keys.toArray

  private def initialLibSameThanCompatible(initial: Lib213, compatible: CompatibleWithScala3Lib): Boolean =
    initial.name == compatible.name && initial.revision == compatible.revision

  override def getMigratedCompilerPlugins: jutil.Map[Lib, String] = compilerPluginsWithScalacOption.collect {
    case (initial, Some(scalacOption)) =>
      initial.asInstanceOf[Lib] -> scalacOption.scala3Value
  }.asJava

}

object MigratedLibsImpl {
  def from(map: Map[Lib213, Either[Option[Scala3cOption], Seq[CompatibleWithScala3Lib]]]): MigratedLibsImpl = {
    val libs            = map.collect { case (l, Right(compatible)) => l -> compatible }
    val compilerPlugins = map.collect { case (l, Left(scalacOption)) => l -> scalacOption }
    MigratedLibsImpl(libs, compilerPlugins)
  }
}
