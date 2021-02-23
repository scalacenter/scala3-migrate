package migrate

import scala.tools.nsc.interactive.Global
import scala.util.Try

import scala.meta._
import scala.meta.contrib.Trivia
import scala.meta.internal.pc.PrettyPrinter
import scala.meta.tokens.Token

import scalafix.patch.Patch
import scalafix.util.TokenOps
import scalafix.v1._
import utils.CompilerService
import utils.ScalaExtensions.TraversableOnceOptionExtension
import utils.SyntheticHelper

class InferTypes[G <: Global](g: G) {
  def fix(implicit doc: SemanticDocument, compilerService: CompilerService[g.type]): Patch = {

    val patchForExplicitResultTypes = addExplicitResultType()
    val patchForTypeApply           = addTypeApply()

    patchForExplicitResultTypes + patchForTypeApply
  }

  private def addExplicitResultType()(implicit doc: SemanticDocument, compilerSrv: CompilerService[g.type]): Patch =
    doc.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body) => {
        fixDefinition(t, name, body)
      }
      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body)) =>
        fixDefinition(t, name, body)

      case t @ Defn.Def(mods, name, _, _, None, body) =>
        fixDefinition(t, name, body)
    }.flatten.asPatch

  private def addTypeApply()(implicit doc: SemanticDocument, compilerSrv: CompilerService[g.type]): Patch =
    doc.synthetics.collect { case syn @ TypeApplyTree(function: SemanticTree, typeArguments: List[SemanticType]) =>
      Try {
        for {
          originalTree <- SyntheticHelper.getOriginalTree(syn)
          if (!originalTree.isInstanceOf[Term.ApplyInfix])
          if (!(originalTree.isInstanceOf[Pat] && !originalTree
            .isInstanceOf[Term])) // Never add types on elements that are on the right side of "="
          replace <- if (syn.toString.startsWith("*.apply")) Some(".apply")
                     else if (syn.toString.startsWith("*[")) Some("")
                     else None
          types <- getTypeNameAsSeenByGlobal(originalTree, replace).map(_.map(_.toString()))
          // if we don't know how to express a type, we don't create a patch
        } yield Patch.addRight(originalTree, s"${replace}[${types.mkString(", ")}]")
      }.toOption.flatten
    // if we have two patches on the same originalTree, ex: a[Type1].apply[Type2]
    // since synthetics traverse the file from top to bottom, we create the patches in the same order
    // but when applying them, we need to apply in the reverse order.
    }.flatten.toList.reverse.asPatch

  private def getTypeNameAsSeenByGlobal(origin: Tree, replace: String)(
    implicit compilerSrv: CompilerService[g.type]
  ): Option[List[g.Type]] =
    for {
      term                  <- SyntheticHelper.getTermName(origin)
      gterm                  = if (replace.isEmpty) g.TermName(term.toString()) else g.TermName("apply")
      (globalTree, context) <- compilerSrv.getGlobalTree(origin)
      tree                  <- getTypeApplyTree(globalTree, gterm)
      types                  = tree.args.map(_.tpe.dealias)
      pretty                 = new PrettyPrinter[g.type](g)
      prettyTypes           <- types.map(t => pretty.print(t, context)).sequence
    } yield prettyTypes

  private def getTypeApplyTree(gtree: g.Tree, termName: g.Name): Option[g.TypeApply] =
    gtree.collect {
      case t @ g.TypeApply(fun, _) if fun.isInstanceOf[g.Select] && fun.asInstanceOf[g.Select].name == termName =>
        t
    }.headOption

  private def fixDefinition(defn: Defn, name: Term.Name, body: Term)(
    implicit doc: SemanticDocument,
    compilerSrv: CompilerService[g.type]
  ): Option[Patch] =
    Try {
      for {
        (replace, spaces) <- getReplaceAndSpaces(defn, body)
        context           <- compilerSrv.getContext(name)
        explicitType       = context.tree.symbol.info.finalResultType
        pretty             = new PrettyPrinter[g.type](g)
        prettType         <- pretty.print(explicitType, context)
      } yield Patch.addRight(replace, s"$spaces: ${prettType.toString()}")
    }.toOption.flatten

  private def getReplaceAndSpaces(defn: Defn, body: Term)(implicit doc: SemanticDocument): Option[(Token, String)] = {
    val tokens = doc.tokenList
    import tokens._

    for {
      start    <- defn.tokens.headOption
      end      <- body.tokens.headOption
      lhsTokens = slice(start, end)
      replace  <- lhsTokens.reverseIterator.find(x => !x.is[Token.Equals] && !x.is[Trivia])
      space = {
        if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
        else ""
      }
    } yield (replace, space)
  }

}
