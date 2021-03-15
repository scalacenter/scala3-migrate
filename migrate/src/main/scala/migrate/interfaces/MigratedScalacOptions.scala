package migrate.interfaces

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import migrate.Scala3cOption
import migrate.ScalacOption

case class MigratedScalacOptions(
  notParsed: Seq[ScalacOption.NotParsed],
  specificScala2: Seq[ScalacOption.Specific2],
  scala3cOptions: Seq[Scala3cOption],
  renamed: Seq[ScalacOption.Renamed]
) extends ScalacOptions {
  override def getScala3cOptions: Array[String] = scala3cOptions.flatMap(_.scala3ValueForBuild).toArray

  override def getNotParsed: Array[String] = notParsed.map(_.value).toArray

  override def getSpecificScala2: Array[String] = specificScala2.map(_.scala2Value).toArray

  override def getRenamed: ju.Map[String, String] = renamed.map(r => r.scala2Value -> r.scala3Value).toMap.asJava
}
