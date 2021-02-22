package migrate

import scala.tools.nsc.interactive.Global
import scala.util.Try

import scala.meta.Term.ApplyInfix
import scala.meta.Tree
import scala.meta.internal.pc.PrettyPrinter

import scalafix.patch.Patch
import scalafix.v1._
import utils.CompilerService
import utils.ScalaExtensions._
import utils.SyntheticHelper

class ExplicitImplicitsRule[G <: Global](g: G) {

  def fix(implicit doc: SemanticDocument, compilerService: CompilerService[g.type]): Patch = {
    val implicitParams = doc.synthetics.collect {
      case syn: ApplyTree if (syn.toString.startsWith("*")) =>
        Try {
          for {
            originalTree <- SyntheticHelper.getOriginalTree(syn)
            args         <- getImplicitParams(originalTree)
            toAdd         = "(" + args.mkString(", ") + ")"
          } yield Patch.addRight(originalTree, toAdd)
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

  def getImplicitParams(originalTree: Tree)(implicit compilerSrv: CompilerService[g.type]): Option[List[String]] =
    // for infix methods, we need to rewrite it
    // example a ++ b needs to be rewritten to a.++(b)(implicit)
    // right now the code would produce a ++ b(implicit) which doesn't compile
    if (originalTree.isInstanceOf[ApplyInfix]) None
    else
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

    collectedTree.head.asInstanceOf[g.Apply].fun.isTerm
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
