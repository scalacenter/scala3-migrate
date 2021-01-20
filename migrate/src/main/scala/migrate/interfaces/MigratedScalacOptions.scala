package migrate.interfaces

import migrate.Scala3cOption
import migrate.ScalacOption

case class MigratedScalacOptions(
  notParsed: Seq[ScalacOption.NotParsed],
  specificScala2: Seq[ScalacOption.Specific2],
  migrated: Seq[Scala3cOption]
) extends ScalacOptions {
  override def getMigrated: Array[String] = migrated.flatMap(_.scala3ValueForBuild).toArray

  override def getNotParsed: Array[String] = notParsed.map(_.value).toArray

  override def getSpecificScala2: Array[String] = specificScala2.map(_.scala2Value).toArray
}
