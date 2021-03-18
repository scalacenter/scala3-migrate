package scala.tools.nsc.interactive

import scala.reflect.internal.util.Position
import scala.reflect.internal.util.SourceFile

class GlobalProxyService[G <: Global] private (g: G) {
  def typedTreeAt(pos: Position): G#Tree =
    g.locateTree(pos)

}

object GlobalProxyService {
  def apply[G <: Global](g: G, sourceFile: SourceFile): GlobalProxyService[G] = {
    // load only once the source and type it
    // It's expensive since it load caches and create a RichCompilationUnit
    g.reloadSource(sourceFile)
    g.typedTree(sourceFile, true)
    new GlobalProxyService[G](g)
  }
}
