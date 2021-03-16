package migrate.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

import coursier.Repositories
import migrate.internal.CompatibleWithScala3Lib
import migrate.internal.Lib213
import migrate.internal.LibToMigrate.CrossVersion
import migrate.internal.LibToMigrate.Revision

object CoursierHelper {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val scala3Full                            = "3.0.0-RC1"
  val scala3Binary                          = "3.0.0-RC1"
  val scala213Binary                        = "2.13"

  def getCompatibleForScala3Binary(lib: Lib213): Seq[CompatibleWithScala3Lib] = {
    val revisions = searchRevisionsFor(lib, scala3Binary)
    val all = revisions.map { r =>
      CompatibleWithScala3Lib(lib.organization, lib.name, r, CrossVersion.For2_13Use3("", ""), lib.configurations)
    }
    getNewerRevision(lib, all)
  }
  def getCompatibleForScala3Full(lib: Lib213): Seq[CompatibleWithScala3Lib] = {
    val revisions = searchRevisionsFor(lib, scala3Full)
    val all = revisions.map { r =>
      CompatibleWithScala3Lib(lib.organization, lib.name, r, CrossVersion.Full("", ""), lib.configurations)
    }
    getNewerRevision(lib, all)
  }
  def getCompatibleForBinary213(lib: Lib213): Seq[CompatibleWithScala3Lib] = {
    val revisions = searchRevisionsFor(lib, scala213Binary)
    val all = revisions.map { r =>
      CompatibleWithScala3Lib(lib.organization, lib.name, r, CrossVersion.For3Use2_13("", ""), lib.configurations)
    }
    getNewerRevision(lib, all)
  }

  private def searchRevisionsFor(lib: Lib213, scalaV: String): Seq[Revision] = {
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
  private def getNewerRevision(
    lib: Lib213,
    compatibleLibs: Seq[CompatibleWithScala3Lib]
  ): Seq[CompatibleWithScala3Lib] = {
    val possibleRevisions = compatibleLibs.map(_.revision).zipWithIndex.toMap
    val index             = possibleRevisions.get(lib.revision)
    index.map(compatibleLibs.drop).getOrElse(compatibleLibs)
  }

}
