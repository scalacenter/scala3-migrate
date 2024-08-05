package migrate.internal

import scala.concurrent.ExecutionContext

import coursier.core.Repository
import sbt.librarymanagement._

object CoursierHelper {

  def findNewerVersions(lib: InitialLib, scalaVersion: String, repositories: Seq[Repository]): Seq[String] = {
    val versions = findAllVersions(lib, scalaVersion, repositories)
    dropOlderVersions(lib.version, versions)
  }

  def isCompatible(lib: InitialLib, scalaVersion: String, repositories: Seq[Repository]): Boolean = {
    val binaryVersion = prefix(lib.crossVersion) + scalaVersion + suffix(lib.crossVersion)
    val libString     = s"${lib.organization}:${lib.name}_$binaryVersion:${lib.version}"
    coursierComplete(libString, repositories).nonEmpty
  }

  private def findAllVersions(lib: InitialLib, scalaVersion: String, repositories: Seq[Repository]): Seq[String] = {
    val binaryVersion = prefix(lib.crossVersion) + scalaVersion + suffix(lib.crossVersion)
    val libString     = s"${lib.organization}:${lib.name}_${binaryVersion}:"
    coursierComplete(libString, repositories)
  }

  private def coursierComplete(input: String, repositories: Seq[Repository]): Seq[String] = {
    val res = coursier.complete
      .Complete()
      .withRepositories(repositories)
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

  private def prefix(crossVersion: CrossVersion): String = crossVersion match {
    case v: Binary      => v.prefix
    case v: Full        => v.prefix
    case v: For3Use2_13 => v.prefix
    case v: For2_13Use3 => v.prefix
    case _              => ""
  }

  private def suffix(crossVersion: CrossVersion): String = crossVersion match {
    case v: Binary      => v.suffix
    case v: Full        => v.suffix
    case v: For3Use2_13 => v.suffix
    case v: For2_13Use3 => v.suffix
    case _              => ""
  }
}
