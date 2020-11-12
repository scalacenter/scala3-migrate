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

    val implicitParams = doc.synthetics.collect {
      case syn: ApplyTree if (syn.toString.startsWith("*")) =>
        (for {
          originalTree <- SyntheticHelper.getOriginalTree(syn)
          args         <- getImplicitParams(originalTree)
        } yield Patch.addRight(originalTree, "(" + args.mkString(", ") + ")")).getOrElse(Patch.empty)
    }.toList.asPatch

    val implicitConversion = doc.synthetics.collect {
      case syn: ApplyTree if syn.toString().contains("(*)") =>
        (for {
          originalTree <- SyntheticHelper.getOriginalTree(syn)
          args         <- getImplicitConversions(originalTree)
//              .orElse(syn.toString.split('(').headOption)
        } yield Patch.addAround(originalTree, args + "(", ")")).getOrElse(Patch.empty)
    }.toList.asPatch

    implicitParams + implicitConversion
  }

  def getImplicitParams(originalTree: Tree)(implicit unit: g.CompilationUnit): Option[List[String]] =
    for {
      context <- CompilerService.getContext(originalTree, g)
      symbols <- collectImplicit(context.tree)
      args    <- symbols.map(printSymbol).sequence
    } yield args

  def getImplicitConversions(originalTree: Tree)(implicit unit: g.CompilationUnit): Option[String] =
    for {
      tree     <- CompilerService.getGlobalTree(originalTree, g)
      function <- collectImplicitConversion(tree)
    } yield function

  private def collectImplicitConversion(globalTree: g.Tree): Option[String] =
    globalTree match {
      case t @ g.Apply(fun: g.Select, _) if t.isInstanceOf[g.ApplyImplicitView] =>
        if (fun.qualifier.symbol.isStatic) Some(fun.name.toString)
        else Some(fun.toString)
      case t @ g.Apply(fun: g.TypeApply, _) if t.isInstanceOf[g.ApplyImplicitView] =>
        if (fun.symbol.isImplicit) Some(fun.symbol.fullName.toString)
        else Some(fun.symbol.name.toString)
      case t @ g.Select(qualifier: g.ApplyImplicitView, _) =>
        collectImplicitConversion(qualifier)
      case _ => None
    }

  private def printSymbol(symbol: g.Symbol): Option[String] =
    if (symbol.isLocalToBlock && !symbol.name.startsWith("evidence$"))
      Some(symbol.name.toString())
    else if (symbol.isStatic) Some(symbol.fullName)
    else None

  private def collectImplicit(globalTree: g.Tree): Option[List[g.Symbol]] =
    globalTree match {
      case g.Apply(_, args) if globalTree.isInstanceOf[g.ApplyToImplicitArgs] => {
        val listOfArgs = args.map(_.symbol.asInstanceOf[g.Symbol])
        Some(listOfArgs)
      }
      case g.Apply(_, args) if args.nonEmpty && args.head.isInstanceOf[g.ApplyToImplicitArgs] => {
        val newArgs    = args.head.asInstanceOf[g.Apply].args
        val listOfArgs = newArgs.map(_.symbol.asInstanceOf[g.Symbol])
        Some(listOfArgs)
      }
      case t =>
        None
    }

}
