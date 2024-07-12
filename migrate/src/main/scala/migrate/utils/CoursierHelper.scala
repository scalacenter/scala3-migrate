package migrate.utils

import scala.concurrent.ExecutionContext

import coursier.MavenRepository
import migrate.internal.InitialLib
import migrate.internal.Repository

object CoursierHelper {

  def findNewerVersions(lib: InitialLib, scalaVersion: String, repositories: Seq[Repository]): Seq[String] = {
    val versions = findAllVersions(lib, scalaVersion, repositories)
    dropOlderVersions(lib.version, versions)
  }

  def isCompatible(lib: InitialLib, scalaVersion: String, repositories: Seq[Repository]): Boolean = {
    val binaryVersion = lib.crossVersion.prefix + scalaVersion + lib.crossVersion.suffix
    val libString     = s"${lib.organization}:${lib.name}_$binaryVersion:${lib.version}"
    coursierComplete(libString, repositories).nonEmpty
  }

  private def findAllVersions(lib: InitialLib, scalaVersion: String, repositories: Seq[Repository]): Seq[String] = {
    val binaryVersion = lib.crossVersion.prefix + scalaVersion + lib.crossVersion.suffix
    val libString     = s"${lib.organization}:${lib.name}_${binaryVersion}:"
    coursierComplete(libString, repositories)
  }

  private def coursierComplete(input: String, repositories: Seq[Repository]): Seq[String] = {
    val res = coursier.complete
      .Complete()
      .withRepositories(repositories.map(r => MavenRepository(r.url)))
      .withInput(input)
      .result()
      .unsafeRun()(ExecutionContext.global)
      .results
    res.flatMap {
      case (_, Right(versions)) => versions
      case _                    => Nil
    }
  }

  // Rely on coursier order
  private def dropOlderVersions(initial: String, allVersions: Seq[String]): Seq[String] = {
    val index = allVersions.indexOf(initial)
    index match {
      case -1 => allVersions
      case x  => allVersions.drop(x)
    }
  }

}
