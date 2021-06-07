package migrate.interfaces

import java.{ util => jutil }

import scala.jdk.CollectionConverters._

import migrate.internal.InitialLib
import migrate.internal.MigratedLib._

case class MigratedLibsImpl(
  compatibleWithScala3: Map[InitialLib, CompatibleWithScala3],
  uncompatibleWithScala3: Map[InitialLib, UncompatibleWithScala3]
) extends MigratedLibs {

  private val (validLibs, toUpdate) = compatibleWithScala3.partition { case (initial, compatible) =>
    initialLibSameThanCompatible(initial, compatible)
  }

  override def getUncompatibleWithScala3: Array[MigratedLib] =
    uncompatibleWithScala3.values.map(_.asInstanceOf[MigratedLib]).toArray

  override def getLibsToUpdate: jutil.Map[Lib, MigratedLib] =
    // to Java ^^
    toUpdate.map { case (initial, compatible) =>
      initial.asInstanceOf[Lib] -> compatible.asInstanceOf[MigratedLib]
    }.asJava

  override def getValidLibs: Array[MigratedLib] = validLibs.values.map(_.asInstanceOf[MigratedLib]).toArray

  private def initialLibSameThanCompatible(initialLib: InitialLib, compatible: CompatibleWithScala3): Boolean =
    compatible match {
      case keptInitialLib: CompatibleWithScala3.KeptInitialLib
          if initialLib.crossVersion == keptInitialLib.crossVersion =>
        true
      case _ => false
    }
  def allLibs: Map[InitialLib, MigratedLib] = compatibleWithScala3 ++ uncompatibleWithScala3
}

object MigratedLibsImpl {
  def from(map: Map[InitialLib, MigratedLib]): MigratedLibsImpl = {
    val libs         = map.collect { case (l, compatible: CompatibleWithScala3) => l -> compatible }
    val uncompatible = map.collect { case (initial, uncompatible: UncompatibleWithScala3) => initial -> uncompatible }
    MigratedLibsImpl(libs, uncompatible)
  }
}
