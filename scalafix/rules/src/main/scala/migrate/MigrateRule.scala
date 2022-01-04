package migrate

import scala.tools.nsc.interactive.Global
import scala.tools.nsc.reporters.StoreReporter
import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import utils.CompilerService

class MigrateRule(g: Global) extends SemanticRule("MigrationRule") {
  override def description: String = "infer types"

  def this() = this(null)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (config.scalacClasspath.isEmpty) {
      Configured.error(s"config.scalacClasspath should not be empty")
    } else {
      val global = CompilerService.newGlobal(config.scalacClasspath, config.scalacOptions)
      global match {
        case Success(settings) =>
          Configured.ok(new MigrateRule(new Global(settings, new StoreReporter, "scala3-migrate")))
        case Failure(exception) => Configured.error(exception.getMessage)
      }
    }

  override def afterComplete(): Unit =
    try {
      g.askShutdown()
      g.close()
    } catch {
      case NonFatal(_) =>
    }

  override def fix(implicit doc: SemanticDocument): Patch = {
    implicit lazy val compilerService: CompilerService[g.type] = new CompilerService(g, doc)

    val inferType        = new InferTypes[g.type](g)
    val explicitImplicit = new ExplicitImplicitsRule[g.type](g)
    inferType.fix + explicitImplicit.fix
  }
}
