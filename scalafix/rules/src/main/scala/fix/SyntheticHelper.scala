package fix

import scalafix.Patch
import scalafix.v1.{SemanticDocument, SemanticTree}

import scala.meta.Tree

object SyntheticHelper {
  def buildPatch(tree: Tree, s: List[SemanticTree])(implicit doc: SemanticDocument): Patch = {
    val semanticTree = s.head // CHECK IF IT's ALWAYS ONE
    assert(s.length == 1 , s"SHOULD SYNTHETICS ALWAYS BE A ONE ELEMENT LIST, BUT IT'S NOT $s")

    val place = Place.where('*', semanticTree)
    val semanticTreeString = semanticTree.toString()
    place match {
      case Place.Left => {
        val value = semanticTreeString.drop(1)
        Patch.addRight(tree, value)
      }
      case Place.Right =>
        val value = semanticTreeString.dropRight(1)
        Patch.addLeft(tree, value)

      case Place.Middle(pos) =>
        val (left, right) = semanticTreeString.splitAt(pos)
        Patch.addRight(tree, left).+(Patch.addLeft(tree, right)) // won't work. We need to create only one patch
      case Place.NoWhere => Patch.empty
    }
  }
}
