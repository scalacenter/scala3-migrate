package migrate

import scala.tools.nsc.interactive.Global
import scala.tools.nsc.reporters.StoreReporter
import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal

import scala.meta.Tree

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.v1._
import utils.CompilerService
import utils.ScalaExtensions._
import utils.SyntheticHelper

class ExplicitImplicitsRule(g: Global) extends SemanticRule("ExplicitImplicits") {
  override def description: String = "Show implicit parameters and conversions"

  def this() = this(null)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (config.scalacClasspath.isEmpty) {
      Configured.error(s"config.scalacClasspath should not be empty")
    } else {
      val global = CompilerService.newGlobal(config.scalacClasspath, config.scalacOptions)
      global match {
        case Success(settings) =>
          Configured.ok(new ExplicitImplicitsRule(new Global(settings, new StoreReporter, "ExplicitImplicit Rule")))
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
    lazy implicit val unit: g.CompilationUnit = g.newCompilationUnit(doc.input.text, doc.input.syntax)

    doc.synthetics.flatMap {
      case syn @ ApplyTree(function, arguments) =>
        // case for implicit params
        if (syn.toString.startsWith("*")) {
          for {
            originalTree <- SyntheticHelper.getOriginalTree(syn)
            args         <- getImplicitParams(originalTree)
          } yield Patch.addRight(originalTree, "(" + args.mkString(", ") + ")")
        }
        // case for implicit conversions
        else if (syn.toString().contains("(*)")) {
          None
        } else None
      case _ => None
    }.toList.asPatch
  }

  def getImplicitParams(originalTree: Tree)(implicit unit: g.CompilationUnit): Option[List[String]] =
    for {
      globalTree <- CompilerService.getContext(originalTree, g)
      args       <- collectImplicit(globalTree.tree)
    } yield args

  private def printSymbol(symbol: g.Symbol): Option[String] =
    if (symbol.isLocalToBlock && !symbol.name.startsWith("evidence$"))
      Some(symbol.name.toString())
    else if (symbol.isStatic) Some(symbol.fullName)
    else None

  private def collectImplicit(globalTree: g.Tree): Option[List[String]] =
    globalTree match {
      case g.Apply(_, args) if globalTree.isInstanceOf[g.ApplyToImplicitArgs] => {
        val listOfArgs = args.map(_.symbol.asInstanceOf[g.Symbol])
        listOfArgs.map(printSymbol).sequence
      }
      case g.Apply(_, args) if args.nonEmpty && args.head.isInstanceOf[g.ApplyToImplicitArgs] => {
        val newArgs    = args.head.asInstanceOf[g.Apply].args
        val listOfArgs = newArgs.map(_.symbol.asInstanceOf[g.Symbol])
        listOfArgs.map(printSymbol).sequence
      }
      case t =>
        None
    }

}
