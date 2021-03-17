package migrate.interfaces

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import migrate.internal.Scala3cOption
import migrate.internal.ScalacOption
import migrate.internal.ScalacOption.PluginSpecific

case class MigratedScalacOptions(
  notParsed: Seq[ScalacOption.NotParsed],
  specificScala2: Seq[ScalacOption.Specific2],
  scala3cOptions: Seq[Scala3cOption],
  renamed: Seq[ScalacOption.Renamed],
  pluginsOptions: Seq[PluginSpecific]
) extends ScalacOptions {
  override def getScala3cOptions: Array[String] = scala3cOptions.flatMap(_.scala3ValueForBuild).toArray

  override def getNotParsed: Array[String] = notParsed.flatMap(_.valueForBuild).toArray

  override def getSpecificScala2: Array[String] = specificScala2.flatMap(_.scala2ValueForBuild).toArray

  override def getPluginsOptions: Array[String] = pluginsOptions.map(_.scala3Value).toArray

  override def getRenamed: ju.Map[String, String] = renamed.map(r => r.scala2Value -> r.scala3Value).toMap.asJava
}
