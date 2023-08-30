package migrate

import scala.tools.nsc.interactive.Global
import scala.util.Try

import scala.meta.Name
import scala.meta.Term
import scala.meta.Term.ApplyInfix
import scala.meta.Tree
import scala.meta.internal.pc.PrettyPrinter

import scalafix.patch.Patch
import scalafix.v1._
import utils.CompilerService
import utils.ScalaExtensions._
import utils.SyntheticHelper

class ExplicitImplicitsRule[G <: Global](g: G) {
  private val printer = new PrettyPrinter[g.type](g)

  def fix(implicit doc: SemanticDocument, compilerService: CompilerService[g.type]): Patch = {
    val implicitParams = doc.synthetics.collect {
      case syn: ApplyTree if (syn.toString.startsWith("*")) =>
        Try {
          for {
            originalTree <- SyntheticHelper.getOriginalTree(syn)
            patch        <- getImplicitParams(originalTree)
          } yield patch
        }.toOption.flatten
    }.flatten.toList.asPatch

    val implicitConversion = doc.synthetics.collect {
      case syn: ApplyTree if syn.toString().contains("(*)") =>
        Try {
          for {
            originalTree <- SyntheticHelper.getOriginalTree(syn)
            args         <- getImplicitConversions(originalTree)
          } yield Patch.addAround(originalTree, args + "(", ")")
        }.toOption.flatten
    }.flatten.toList.asPatch

    implicitParams + implicitConversion
  }

  def getImplicitParams(originalTree: Tree)(implicit compilerSrv: CompilerService[g.type]): Option[Patch] =
    // for infix methods, we need to rewrite it
    // example a ++ b needs to be rewritten to a.++(b)(implicit)
    // right now the code would produce a ++ b(implicit) which doesn't compile
    if (originalTree.isInstanceOf[ApplyInfix]) None
    else
      for {
        context             <- compilerSrv.getContext(originalTree)
        (funcTree, symbols) <- collectImplicit(context.tree, originalTree)
        args                <- symbols.map(gsym => printer.print(gsym, context)).sequence
        patch               <- implicitParamsPatch(funcTree, originalTree, args)
      } yield patch

  def getImplicitConversions(originalTree: Tree)(implicit compilerSrv: CompilerService[g.type]): Option[String] =
    for {
      (tree, _) <- compilerSrv.getGlobalTree(originalTree)
      function  <- collectImplicitConverter(tree, originalTree)
    } yield function

  private def collectImplicitConverter(globalTree: g.Tree, originalTree: Tree): Option[String] = {
    def funOf(tree: g.Tree): g.Tree = tree match {
      case g.Apply(fun, _)     => funOf(fun)
      case g.TypeApply(fun, _) => funOf(fun)
      case tree                => tree
    }

    def skipPackageObject(sym: g.Symbol): g.Symbol =
      if (sym.isPackageObject) sym.owner
      else sym

    globalTree.collect {
      case t: g.ApplyImplicitView if CompilerService.equalForPositions(t.pos, originalTree.pos) =>
        funOf(t) match {
          case fun @ g.Select(qual, _) if qual.symbol.isStatic =>
            skipPackageObject(qual.symbol).fullName.toString + "." + fun.name.toString
          case fun => fun.toString
        }
    }.headOption
  }

  private def collectImplicit(globalTree: g.Tree, original: Tree): Option[(G#Tree, List[g.Symbol])] = {
    val collectedTree: Seq[g.Tree] =
      globalTree.collect {
        case t if CompilerService.equalForPositions(t.pos, original.pos) => t
      }.filter(_.isInstanceOf[g.ApplyToImplicitArgs])

    collectedTree.collectFirst {
      // at the same position we are supposed to have maximum one ApplyToImplicitArgs
      // except it there is also an implicit conversion that takes implicits.
      // See Mix.scala example
      case g.Apply(fun, args) if !fun.isInstanceOf[g.ApplyImplicitView] =>
        val listOfArgs = args.map(_.symbol.asInstanceOf[g.Symbol])
        (fun, listOfArgs)
    }

  }

  private def implicitParamsPatch(globalTree: G#Tree, original: Tree, implicitsParams: List[String]): Option[Patch] =
    globalTree match {
      //  def foo2(a: Int)(b: Int)(implicit value: Int): Int = a + b
      //  def bar(f: Int => Int => Int): Int = ???
      //  need a rewrite like this bar(a => b =>  foo(a)(b)(implicit))
      // we don't wont to handle this rewrite.
      case g.Apply(g.Apply(_, _), _) if original.isInstanceOf[Name] || original.isInstanceOf[Term.Select] =>
        None
      case g.Apply(_, args) if original.isInstanceOf[Name] || original.isInstanceOf[Term.Select] =>
        val isTermEta        = original.parent.exists(_.isInstanceOf[Term.Eta])
        val etaExpansionArgs = (1 to args.size).map(_ => "_")
        if (isTermEta) {
          original.parent.map { parent =>
            (Patch.replaceTree(parent, original.toString()) + Patch
              .addRight(
                parent,
                s"(${etaExpansionArgs.mkString(", ")})" + "(" + implicitsParams.mkString(", ") + ")"
              )).atomic
          }
        } else
          Some(
            Patch
              .addRight(original, s"(${etaExpansionArgs.mkString(", ")})" + "(" + implicitsParams.mkString(", ") + ")")
          )
      case _ => Some(Patch.addRight(original, "(" + implicitsParams.mkString(", ") + ")"))
    }
}
