package migrate.utils

import scala.concurrent.ExecutionContext

import coursier.Repositories
import migrate.internal.InitialLib

object CoursierHelper {

  def findNewerVersions(lib: InitialLib, scalaVersion: String): Seq[String] = {
    val versions = findAllVersions(lib, scalaVersion)
    dropOlderVersions(lib.version, versions)
  }

  def isCompatible(lib: InitialLib, scalaVersion: String): Boolean = {
    val binaryVersion = lib.crossVersion.prefix + scalaVersion + lib.crossVersion.suffix
    val libString     = s"${lib.organization}:${lib.name}_$binaryVersion:${lib.version}"
    coursierComplete(libString).nonEmpty
  }

  private def findAllVersions(lib: InitialLib, scalaVersion: String): Seq[String] = {
    val binaryVersion = lib.crossVersion.prefix + scalaVersion + lib.crossVersion.suffix
    val libString     = s"${lib.organization}:${lib.name}_${binaryVersion}:"
    coursierComplete(libString)
  }

  private def coursierComplete(input: String): Seq[String] = {
    val res = coursier.complete
      .Complete()
      .withRepositories(Seq(Repositories.central))
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
