package migrate.interfaces

import java.{ util => jutil }

import scala.jdk.CollectionConverters._

import migrate.CompatibleWithScala3Lib
import migrate.Lib213
import migrate.Scala3cOption

case class MigratedLibsImpl(
  libs: Map[Lib213, Seq[CompatibleWithScala3Lib]],
  compilerPlugins: Map[Lib213, Option[Scala3cOption]]
) extends MigratedLibs {
  private val (nonMigratedLibs, migrated) = libs.partition { case (_, migrated) => migrated.isEmpty }
  private val (compilerPluginsWithScalacOption, compilerPluginsWithout) = compilerPlugins.partition {
    case (_, scalacOption) =>
      scalacOption.isDefined
  }

  override def getNotMigrated: Array[Lib] =
    ((nonMigratedLibs.keys ++ compilerPluginsWithout.keys).map(_.asInstanceOf[Lib]).toArray)
  override def getMigrated: jutil.Map[Lib, jutil.List[Lib]] =
    // to Java ^^
    migrated.map { case (initial, compatible) =>
      initial.asInstanceOf[Lib] -> compatible.map(_.asInstanceOf[Lib]).asJava
    }.asJava

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
