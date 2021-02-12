package migrate

import scala.tools.nsc.interactive.Global
import scala.tools.nsc.reporters.StoreReporter
import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal

import scala.meta.Tree
import scala.meta.internal.pc.PrettyPrinter

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
    lazy implicit val compilerSrv: CompilerService[g.type] = new CompilerService[g.type](g, doc)

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

  def getImplicitParams(originalTree: Tree)(implicit compilerSrv: CompilerService[g.type]): Option[List[String]] =
    for {
      context <- compilerSrv.getContext(originalTree)
      symbols <- collectImplicit(context.tree, originalTree)
      pretty   = new PrettyPrinter[g.type](g)
      args    <- symbols.map(gsym => pretty.print(gsym, context)).sequence
    } yield args

  def getImplicitConversions(originalTree: Tree)(implicit compilerSrv: CompilerService[g.type]): Option[String] =
    for {
      (tree, _) <- compilerSrv.getGlobalTree(originalTree)
      function  <- collectImplicitConversion(tree, originalTree)
    } yield function

  private def collectImplicitConversion(globalTree: g.Tree, original: Tree): Option[String] = {
    val collectedTree: Seq[g.Tree] =
      globalTree.collect {
        case t if CompilerService.equalForPositions(t.pos, original.pos) => t
      }.filter(_.isInstanceOf[g.ApplyImplicitView])

    collectedTree.collectFirst {
      case t @ g.Apply(fun: g.Select, _) =>
        if (fun.qualifier.symbol.isStatic) fun.name.toString
        else fun.toString
      case t @ g.Apply(fun: g.TypeApply, _) =>
        if (fun.symbol.isStatic) fun.symbol.fullName.toString
        else fun.symbol.name.toString
    }

  }

  private def collectImplicit(globalTree: g.Tree, original: Tree): Option[List[g.Symbol]] = {
    val collectedTree: Seq[g.Tree] =
      globalTree.collect {
        case t if CompilerService.equalForPositions(t.pos, original.pos) => t
      }.filter(_.isInstanceOf[g.ApplyToImplicitArgs])

    collectedTree.collectFirst {
      // at the same position we are supposed to have maximum one ApplyToImplicitArgs
      // except it there is also an implicit conversion that takes implicits.
      // See Mix.scala example
      case g.Apply(fun, args) if !fun.isInstanceOf[g.ApplyImplicitView] => {
        val listOfArgs = args.map(_.symbol.asInstanceOf[g.Symbol])
        listOfArgs
      }
    }

  }

}
