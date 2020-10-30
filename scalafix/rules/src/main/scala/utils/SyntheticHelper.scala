package utils

import scala.meta.Term
import scala.meta.Tree

import scalafix.v1.ApplyTree
import scalafix.v1.FunctionTree
import scalafix.v1.IdTree
import scalafix.v1.LiteralTree
import scalafix.v1.MacroExpansionTree
import scalafix.v1.NoTree
import scalafix.v1.OriginalSubTree
import scalafix.v1.OriginalTree
import scalafix.v1.SelectTree
import scalafix.v1.SemanticTree
import scalafix.v1.TypeApplyTree

object SyntheticHelper {

  def getOriginalTree(semanticTree: SemanticTree): Option[Tree] =
    semanticTree match {
      case IdTree(info)              => None
      case SelectTree(qualifier, id) => getOriginalTree(qualifier)
      case ApplyTree(function, arguments) =>
        getOriginalTree(function)
          .orElse(arguments.map(getOriginalTree).head)
      case TypeApplyTree(function, typeArguments) => getOriginalTree(function)

      case FunctionTree(parameters, body)           => getOriginalTree(body)
      case LiteralTree(constant)                    => None
      case MacroExpansionTree(beforeExpansion, tpe) => None
      case OriginalSubTree(tree)                    => Some(tree)
      case OriginalTree(tree)                       => Some(tree)
      case NoTree                                   => None
    }

  def getTermName(origin: Tree): Option[Term.Name] =
    origin match {
      case Term.Select(_, name)               => Some(name)
      case t: Term.Name                       => Some(t)
      case Term.Apply(name: Term.Name, _)     => Some(name)
      case Term.ApplyType(name: Term.Name, _) => Some(name)
      case _                                  => None
    }
}
