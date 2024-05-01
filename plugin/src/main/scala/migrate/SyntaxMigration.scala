package migrate

import migrate.ScalaMigratePlugin.Keys._

import sbt.Keys._
import sbt._

import scala.io.AnsiColor._
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConverters._

private[migrate] object SyntaxMigration {
  val internalImpl = Def.taskDyn {
    val configs   = migrationConfigs.value
    val projectId = thisProject.value.id
    val sv        = scalaVersion.value
    val log       = streams.value.log

    if (!sv.startsWith("2.13."))
      throw new MessageOnlyException(Messages.notScala213(sv, projectId))

    log.info(s"${BOLD}Starting migration of syntax in project '$projectId'$RESET")

    Def.task {
      val log             = streams.value.log
      val _               = configs.map(_ / compile).join.value
      val allScala2Inputs = configs.map(_ / scala2Inputs).join.value
      val baseDirectory   = Keys.baseDirectory.value

      val migrateAPI = ScalaMigratePlugin.getMigrateInstance(log)
      for {
        (scala2Inputs, config) <- allScala2Inputs.zip(configs)
        if scala2Inputs.unmanagedSources.nonEmpty
      } Try {
        migrateAPI.migrateSyntax(
          scala2Inputs.unmanagedSources.asJava,
          scala2Inputs.semanticdbTarget,
          scala2Inputs.classpath.asJava,
          scala2Inputs.scalacOptions.asJava,
          baseDirectory.toPath
        )
      } match {
        case scala.util.Success(_) =>
        case Failure(cause) =>
          log.error(s"Failed migration of syntax in $projectId / $config")
          throw cause
      }

      log.info(s"Migration of syntax in $projectId succeeded.")
    }
  }
}
