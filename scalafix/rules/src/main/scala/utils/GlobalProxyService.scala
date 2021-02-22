package scala.tools.nsc.interactive

import scala.reflect.internal.util.Position
import scala.reflect.internal.util.SourceFile

class GlobalProxyService[G <: Global](g: G, sourceFile: SourceFile) {
  // load only once the source and type it
  // It's expensive since it load caches and create a RichCompilationUnit
  private val _ = g.reloadSource(sourceFile)
  private val _ = g.typedTree(sourceFile, true)

  def typedTreeAt(pos: Position): G#Tree =
    g.locateTree(pos)

}
