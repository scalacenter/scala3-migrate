package migrate.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

import buildinfo.BuildInfo
import coursier.Repositories
import migrate.interfaces.InitialLibImp._
import migrate.interfaces.MigratedLibImp.Reason
import migrate.internal.InitialLib
import migrate.internal.MigratedLib._

object CoursierHelper {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val scala3Full                            = BuildInfo.scala3Version
  val scala3Binary                          = "3"
  val scala213Binary                        = "2.13"

  def getCompatibleForScala3Binary(lib: InitialLib): Option[CompatibleWithScala3.Lib] = {
    val revisions = searchRevisionsFor(lib, scala3Binary)
    if (revisions.isEmpty) None
    else {
      val all = CompatibleWithScala3.Lib(
        lib.organization,
        lib.name,
        revisions,
        CrossVersion.For2_13Use3("", ""),
        lib.configurations,
        Reason.Scala3LibAvailable
      )
      Some(getNewerRevision(lib, all))
    }
  }
  def getCompatibleForScala3Full(lib: InitialLib): Option[CompatibleWithScala3.Lib] = {
    val revisions = searchRevisionsFor(lib, scala3Full)
    if (revisions.isEmpty) None
    else {
      val all = CompatibleWithScala3.Lib(
        lib.organization,
        Name(lib.name.value + s"_${CoursierHelper.scala3Full}"),
        revisions,
        CrossVersion.Full("", ""),
        lib.configurations,
        Reason.Scala3LibAvailable
      )
      Some(getNewerRevision(lib, all))
    }
  }

  private def searchRevisionsFor(lib: InitialLib, scalaV: String): Seq[Revision] = {
    val libString = s"${lib.organization.value}:${lib.name.value}_$scalaV:"
    val res = coursier.complete
      .Complete()
      .withRepositories(Seq(Repositories.central))
      .withInput(libString)
      .result()
      .unsafeRun()(ec)
      .results
    val revisions = res.flatMap {
      case (_, Right(revisions)) => revisions
      case _                     => Nil
    }
    revisions.map(Revision(_))
  }

  // Rely on coursier order
  private def getNewerRevision(lib: InitialLib, compatibleLibs: CompatibleWithScala3.Lib): CompatibleWithScala3.Lib = {
    val revisions = compatibleLibs.revisions
    val index     = revisions.zipWithIndex.toMap.get(lib.revision)
    index match {
      case Some(value) =>
        val keptRevisions = revisions.drop(value)
        compatibleLibs.copy(revisions = keptRevisions)
      case None => compatibleLibs
    }
  }

}
