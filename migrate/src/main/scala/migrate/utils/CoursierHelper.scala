package migrate.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

import buildinfo.BuildInfo
import coursier.Repositories
import migrate.interfaces.InitialLibImp._
import migrate.internal.InitialLib
import migrate.internal.ScalaVersion

object CoursierHelper {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val scala3Full: ScalaVersion              = ScalaVersion.from(BuildInfo.scala3Version).get

  def getCompatibleForScala3Binary(lib: InitialLib): Seq[Revision] = {
    val revisions = searchRevisionsFor(lib, scala3Full.binary)
    if (revisions.isEmpty) Nil
    else {
      getNewerRevision(lib, revisions)
    }
  }
  def getCompatibleForScala3Full(lib: InitialLib): Seq[Revision] = {
    val revisions = searchRevisionsFor(lib, scala3Full.value)
    if (revisions.isEmpty) Nil
    else {
      getNewerRevision(lib, revisions)
    }
  }

  def isRevisionAvailableFor(lib: InitialLib, revision: Revision, scalaVersion: ScalaVersion): Boolean = {
    val input = s"${lib.organization.value}:${lib.name.value}_${scalaVersion.value}:${revision.value}"
    coursierComplete(input).nonEmpty
  }

  private def searchRevisionsFor(lib: InitialLib, scalaV: String): Seq[Revision] = {
    val libString = s"${lib.organization.value}:${lib.name.value}_$scalaV:"
    coursierComplete(libString)
  }
  private def coursierComplete(input: String): Seq[Revision] = {
    val res = coursier.complete
      .Complete()
      .withRepositories(Seq(Repositories.central))
      .withInput(input)
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
  private def getNewerRevision(lib: InitialLib, possibleRevisions: Seq[Revision]): Seq[Revision] = {
    val index = possibleRevisions.zipWithIndex.toMap.get(lib.revision)
    index match {
      case Some(value) =>
        possibleRevisions.drop(value)
      case None => possibleRevisions
    }
  }

}
