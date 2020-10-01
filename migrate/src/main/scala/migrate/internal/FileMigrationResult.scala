package migrate.internal

import scalafix.interfaces.ScalafixPatch

private[migrate] trait FileMigrationResult
private[migrate] case class FileMigrationSuccess(patches: Seq[ScalafixPatch]) extends FileMigrationResult
private[migrate] case class FileMigrationFailure(cause: Throwable) extends FileMigrationResult
