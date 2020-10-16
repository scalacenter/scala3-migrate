package migrate

import scalafix.v1._

import scala.meta.Tree

object SyntheticHelper {

  def getOriginalTree(semanticTree: SemanticTree): Option[Tree] = {
    semanticTree match {
      case IdTree(info) => None
      case SelectTree(qualifier, id) => getOriginalTree(qualifier)
      case ApplyTree(function, arguments) => getOriginalTree(function)
      case TypeApplyTree(function, typeArguments) => getOriginalTree(function)

      case FunctionTree(parameters, body) => getOriginalTree(body)
      case LiteralTree(constant) => None
      case MacroExpansionTree(beforeExpansion, tpe) => None
      case OriginalSubTree(tree) => Some(tree)
      case OriginalTree(tree) => Some(tree)
      case NoTree => None
    }
  }
}
